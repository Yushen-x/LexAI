package com.lexai.backend.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractDraftRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractDraftResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.ContractRiskItem;
import com.lexai.backend.application.dto.response.RetrievalContext;
import com.lexai.backend.application.port.out.LegalReasoningGateway;
import com.lexai.backend.domain.model.RiskLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 腾讯大模型 + 得理开放平台检索（可选本地知识库）的法律智能网关。
 *
 * 说明：
 * 1) 当未配置腾讯大模型 endpoint 或调用失败时，会自动退化到“模板输出 + 检索结果”策略，保证系统可用。
 * 2) 得理接口解析字段可能因接口返回结构差异而失败，因此也做了多种兜底解析。
 * 3) 合同审查支持二轮自检（ReAct 思想）：第一轮先出审查结论，第二轮自检后修正/精炼，再返回。
 */
@Component
@ConditionalOnProperty(name = "lexai.ai.mode", havingValue = "tencent")
public class TencentLegalReasoningGateway implements LegalReasoningGateway {

    private final DeliCaseSearchClient deliCaseSearchClient;
    private final DeliLegalSearchClient deliLegalSearchClient;
    private final LocalKnowledgeSearchClient localKnowledgeSearchClient;
    private final TencentLLMClient tencentLLMClient;
    private final ObjectMapper objectMapper;
    private final boolean reviewSelfCheckEnabled;

    public TencentLegalReasoningGateway(
            DeliCaseSearchClient deliCaseSearchClient,
            DeliLegalSearchClient deliLegalSearchClient,
            LocalKnowledgeSearchClient localKnowledgeSearchClient,
            TencentLLMClient tencentLLMClient,
            ObjectMapper objectMapper,
            @Value("${lexai.ai.review.self-check:true}") boolean reviewSelfCheckEnabled
    ) {
        this.deliCaseSearchClient = deliCaseSearchClient;
        this.deliLegalSearchClient = deliLegalSearchClient;
        this.localKnowledgeSearchClient = localKnowledgeSearchClient;
        this.tencentLLMClient = tencentLLMClient;
        this.objectMapper = objectMapper;
        this.reviewSelfCheckEnabled = reviewSelfCheckEnabled;
    }

    @Override
    public ConsultationResponse consult(ConsultationRequest request) {
        String question = Optional.ofNullable(request.question()).orElse("").trim();
        List<String> facts = request.facts() == null ? Collections.emptyList() : request.facts();

        List<String> lawItems = safeCall(() -> deliLegalSearchClient.searchLaw(question, true), List.of());
        List<String> caseItems = safeCall(() -> deliCaseSearchClient.searchCase(question, 5), List.of());
        List<String> kbItems = safeCall(() -> localKnowledgeSearchClient.searchTopK(question, 3), List.of());

        // 先尝试走“严格结构化JSON”路径；失败则退化为模板输出。
        String systemPrompt = CONSULT_SYSTEM_PROMPT;
        String userPrompt = buildConsultUserPrompt(question, facts, lawItems, caseItems, kbItems);
        String llmText = tencentLLMClient.chat(systemPrompt, userPrompt);
        if (llmText != null) {
            Optional<ConsultationResponse> parsed = tryParseConsultationResponse(llmText);
            if (parsed.isPresent()) {
                return withRetrievalContext(parsed.get(), lawItems, caseItems, kbItems);
            }
        }

        // 兜底策略：用检索结果填充 legalBasis + risk/recommendations。
        String category = inferConsultationCategory(question);
        List<String> legalBasis = lawItems.isEmpty()
                ? List.of("未检索到可用的法条信息；建议提供更具体的事实与争议点以便精确检索。")
                : lawItems;

        List<String> recommendations = List.of(
                "先补充关键信息并固定证据（合同/聊天记录/付款凭证等），再结合检索到的法条与案例进行论证。",
                "围绕争议焦点整理争议点-证据-法律依据的对应关系，形成可用于协商/仲裁/诉讼的要点清单。",
                "如涉及金额较大或时效风险，建议尽快进行人工律师复核与策略调整。"
        );

        List<String> riskAlerts = caseItems.isEmpty()
                ? List.of("当前未检索到相匹配的案例，存在适用性不确定风险；建议补充案件背景或关键词。")
                : List.of(
                        "已检索到相关类案，可重点关注其裁判要旨与判决结果的适用条件，避免照搬导致的偏差。",
                        "注意证据链完整性与关键事实时间线一致性，以降低举证风险。"
                );

        return new ConsultationResponse(
                category,
                legalBasis,
                recommendations,
                riskAlerts,
                0.55,
                buildRetrievalContext(lawItems, caseItems, kbItems),
                buildFallbackAnswer(question, category, lawItems)
        );
    }

    private static String buildFallbackAnswer(String question, String category, List<String> lawItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据你描述的情况，本问题主要属于「").append(category).append("」。");
        if (lawItems != null && !lawItems.isEmpty()) {
            sb.append("可参考下方检索到的相关法条与建议；").append("如果你希望我直接修改合同正文，请切换到右上角的「Agent 修改指令」模式。");
        } else {
            sb.append("当前未检索到强相关法条，建议补充更多事实细节再分析。");
        }
        return sb.toString();
    }

    @Override
    public CaseAnalysisResponse analyzeCase(CaseAnalysisRequest request) {
        String caseSummary = Optional.ofNullable(request.caseSummary()).orElse("").trim();
        List<String> evidencePoints = request.evidencePoints() == null ? Collections.emptyList() : request.evidencePoints();

        List<String> caseItems = safeCall(() -> deliCaseSearchClient.searchCase(caseSummary, 5), List.of());
        List<String> kbItems = safeCall(() -> localKnowledgeSearchClient.searchTopK(caseSummary, 3), List.of());

        String systemPrompt = CASE_ANALYSIS_SYSTEM_PROMPT;
        String userPrompt = buildCaseAnalysisUserPrompt(caseSummary, evidencePoints, caseItems, kbItems);
        String llmText = tencentLLMClient.chat(systemPrompt, userPrompt);
        if (llmText != null) {
            Optional<CaseAnalysisResponse> parsed = tryParseCaseAnalysisResponse(llmText);
            if (parsed.isPresent()) {
                return withRetrievalContext(parsed.get(), List.of(), caseItems, kbItems);
            }
        }

        // 兜底：从证据点与类案/知识库要点中抽象出字段。
        List<String> keyFacts = caseItems.isEmpty()
                ? List.of("已获取案情摘要，建议进一步补充关键时间线、双方主体关系与履约/违约过程。")
                : List.of("已检索到相关类案要点，建议重点比对争议焦点与裁判依据的适用条件。");

        List<String> disputedIssues = List.of(
                "责任承担边界是否清晰（合同约定/法律规定/证据支撑）。",
                "关键争议事实是否有直接证据或可替代证据支撑。",
                "程序性要点是否满足（如管辖、时效、证据提交规则等）。"
        );

        List<String> evidenceGaps = evidencePoints.isEmpty()
                ? List.of("尚未录入具体证据：建议补充合同/付款记录/聊天记录/鉴定或其他直接证明材料。")
                : List.of("建议核验现有证据是否覆盖关键争议事实的证明点，并补足缺失的对方主张反证。");

        List<String> suggestedActions = List.of(
                "整理时间线并建立证据目录，围绕争议焦点逐项匹配证据材料。",
                "形成诉讼或谈判的事实摘要版本，明确每一条主张对应的证据与法律依据。",
                "必要时对关键事实开展进一步补证（取证/调证/申请鉴定）。"
        );

        return new CaseAnalysisResponse(
                keyFacts,
                disputedIssues,
                evidenceGaps,
                suggestedActions,
                0.55,
                buildRetrievalContext(List.of(), caseItems, kbItems)
        );
    }

    @Override
    public ContractReviewResponse reviewContract(ContractReviewRequest request) {
        String contractTitle = Optional.ofNullable(request.contractTitle()).orElse("").trim();
        String contractContent = Optional.ofNullable(request.contractContent()).orElse("").trim();

        // 为降低调用成本，只取前半段拼 prompt。
        String limitedContent = contractContent.length() > 2500 ? contractContent.substring(0, 2500) : contractContent;

        List<String> lawItems = safeCall(() -> deliLegalSearchClient.searchLaw(limitedContent, true), List.of());
        List<String> kbItems = safeCall(() -> localKnowledgeSearchClient.searchTopK(limitedContent, 3), List.of());

        String systemPrompt = CONTRACT_REVIEW_SYSTEM_PROMPT;
        String userPrompt = buildContractReviewUserPrompt(contractTitle, limitedContent, lawItems, kbItems);
        String firstRoundText = tencentLLMClient.chat(systemPrompt, userPrompt);

        if (firstRoundText != null) {
            Optional<ContractReviewResponse> firstParsed = tryParseContractReviewResponse(firstRoundText);
            if (firstParsed.isPresent()) {
                ContractReviewResponse firstRound = firstParsed.get();

                // —— 第二轮：自检（ReAct）——
                // 让模型对自己上一轮输出做一次复核：剔除幻觉、补齐遗漏、调整 confidence。
                if (reviewSelfCheckEnabled) {
                    String selfCheckPrompt = buildSelfCheckUserPrompt(
                            contractTitle, limitedContent, lawItems, kbItems, firstRoundText
                    );
                    String secondRoundText = tencentLLMClient.chat(CONTRACT_REVIEW_SELF_CHECK_SYSTEM_PROMPT, selfCheckPrompt);
                    if (secondRoundText != null) {
                        Optional<ContractReviewResponse> secondParsed = tryParseContractReviewResponse(secondRoundText);
                        if (secondParsed.isPresent()) {
                            return withRetrievalContext(secondParsed.get(), lawItems, List.of(), kbItems);
                        }
                    }
                }

                return withRetrievalContext(firstRound, lawItems, List.of(), kbItems);
            }
        }

        // 兜底：沿用现有 Mock 的规则化审查，并在 summary 里提示“后续接入真正AI后可提升准确性”。
        String contentLower = limitedContent.toLowerCase(Locale.ROOT);
        List<ContractRiskItem> risks = new ArrayList<>();
        List<String> missingClauses = new ArrayList<>();

        if (!contentLower.contains("违约")) missingClauses.add("违约责任条款");
        if (!contentLower.contains("保密")) missingClauses.add("保密条款");
        if (!contentLower.contains("解除")) missingClauses.add("解除与终止条款");
        if (!contentLower.contains("争议解决")) missingClauses.add("争议解决条款");

        if (contentLower.contains("单方解释权") || contentLower.contains("解释权归甲方")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.HIGH,
                    "单方解释权条款",
                    "条款存在明显失衡，可能引发显失公平或无效风险",
                    "删除单方绝对解释权表述，改为双方协商解释或按法律规定处理"
            ));
        }
        if (!contentLower.contains("付款")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.MEDIUM,
                    "付款安排",
                    "缺少付款节点、金额及逾期处理机制，履约争议风险较高",
                    "补充付款条件、付款期限、发票规则及逾期责任"
            ));
        }
        if (!contentLower.contains("交付") && !contentLower.contains("验收")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.MEDIUM,
                    "交付与验收",
                    "缺少交付标准与验收机制，难以界定履约完成节点",
                    "补充交付物清单、验收标准、验收时间与异议处理规则"
            ));
        }

        if (risks.isEmpty()) {
            risks.add(new ContractRiskItem(
                    RiskLevel.LOW,
                    "整体结构",
                    "暂未发现显著高风险条款，但仍建议结合业务事实进行人工复核",
                    "继续补充主体信息、违约责任和争议解决细节"
            ));
        }

        return new ContractReviewResponse(
                risks,
                missingClauses.isEmpty() ? List.of("暂未识别出明显缺失的核心条款；建议结合合同类型进一步核对。") : missingClauses,
                "当前审查结果为“可用兜底”策略：已接入得理检索/本地知识库的上下文能力，但腾讯大模型调用未配置或解析失败时将使用规则化分析。",
                0.5,
                buildRetrievalContext(lawItems, List.of(), kbItems)
        );
    }

    @Override
    public ContractDraftResponse draftContract(ContractDraftRequest request) {
        String contractName = Optional.ofNullable(request.contractName()).orElse("未命名合同").trim();
        String contractType = Optional.ofNullable(request.contractType()).orElse("标准合同").trim();
        String partyA = Optional.ofNullable(request.partyA()).orElse("甲方").trim();
        String partyB = Optional.ofNullable(request.partyB()).orElse("乙方").trim();
        Double amount = request.amount() != null ? request.amount() : 0.0;
        String duration = Optional.ofNullable(request.duration()).orElse("12个月").trim();
        String requirements = Optional.ofNullable(request.requirements()).orElse("详见双方另行商定的明细表").trim();

        // —— 起草阶段 RAG：根据合同类型 + 核心需求检索法规和本地条款知识库 ——
        String retrievalQuery = (contractType + " " + requirements).trim();
        List<String> lawItems = safeCall(() -> deliLegalSearchClient.searchLaw(retrievalQuery, true), List.of());
        List<String> kbItems = safeCall(() -> localKnowledgeSearchClient.searchTopK(retrievalQuery, 3), List.of());

        // 调用大模型生成合同正文
        String systemPrompt = DRAFT_SYSTEM_PROMPT;
        String userPrompt = buildDraftUserPrompt(
                contractName, contractType, partyA, partyB, amount, duration, requirements,
                lawItems, kbItems
        );
        String llmText = tencentLLMClient.chat(systemPrompt, userPrompt);

        if (llmText != null && !llmText.trim().isEmpty()) {
            String summary = generateSummary(llmText);
            return new ContractDraftResponse(
                    contractName,
                    llmText,
                    summary,
                    java.time.LocalDateTime.now(),
                    lawItems.isEmpty() && kbItems.isEmpty() ? 0.7 : 0.85,
                    buildRetrievalContext(lawItems, List.of(), kbItems)
            );
        }

        // 兜底：用模板生成合同（与 Mock 逻辑一致）
        String generatedContent = generateContractTemplate(contractName, contractType, partyA, partyB, amount, duration, requirements);

        return new ContractDraftResponse(
                contractName,
                generatedContent,
                "已生成合同正文（兜底模板），请根据实际情况补充具体条款并进行法律审查。",
                java.time.LocalDateTime.now(),
                0.5,
                buildRetrievalContext(lawItems, List.of(), kbItems)
        );
    }

    private String buildDraftUserPrompt(
            String contractName,
            String contractType,
            String partyA,
            String partyB,
            Double amount,
            String duration,
            String requirements,
            List<String> lawItems,
            List<String> kbItems
    ) {
        return """
# 合同基本信息：
合同名称：%s
合同类型：%s
甲方：%s
乙方：%s
合同金额：¥%s 元
合同期限：%s
核心需求：%s

# 检索到的法条（建议参考但不要硬抄，可为空）：
%s

# 本地条款知识库片段（合同条款样本/合规要点，可为空）：
%s

# 输出要求：
请生成一份完整的、符合中文法律文书规范的合同正文。
合同应包含以下核心条款，且条款表述应与“检索到的法条”及“本地条款知识库片段”保持一致：
1. 合同主体与标的
2. 服务内容与范围
3. 服务期限
4. 费用与支付条款
5. 双方权利义务
6. 保密条款
7. 违约责任
8. 争议解决
9. 其他条款

请直接输出合同正文（不要输出 JSON、不要输出 Markdown 围栏），包括标题、签署日期、甲乙方签名处等。
表述需准确、专业、符合法律规范，避免出现明显失衡条款（如单方解释权）。
""".formatted(
                contractName,
                contractType,
                partyA,
                partyB,
                amount,
                duration,
                requirements,
                formatRagItems(lawItems, "L"),
                formatRagItems(kbItems, "K")
        );
    }

    private String generateSummary(String contractContent) {
        if (contractContent == null) return "合同摘要：生成成功";
        String trimmed = contractContent.replaceAll("\\s+", " ").trim();
        if (trimmed.length() > 300) {
            return trimmed.substring(0, 300) + "...";
        }
        return trimmed;
    }

    private String generateContractTemplate(
            String contractName,
            String contractType,
            String partyA,
            String partyB,
            Double amount,
            String duration,
            String requirements
    ) {
        StringBuilder content = new StringBuilder();
        content.append(contractName).append("\n\n");
        content.append("本合同由以下各方于")
                .append(java.time.LocalDate.now())
                .append("签订：\n\n");

        content.append("甲方（服务提供商）：").append(partyA).append("\n");
        content.append("乙方（服务接收方）：").append(partyB).append("\n\n");

        content.append("鉴于：\n");
        content.append("甲方同意为乙方提供相关服务，乙方同意支付相应费用。双方在平等、自愿、协商一致的基础上，达成如下协议：\n\n");

        content.append("第一条  服务内容与范围\n");
        content.append("1.1 甲方向乙方提供相关的服务。具体服务内容为：")
                .append(requirements)
                .append("\n");
        content.append("1.2 甲方承诺按照约定的时间、质量标准完成上述服务。\n\n");

        content.append("第二条  服务期限\n");
        content.append("2.1 合同服务期限为：").append(duration).append("\n");
        content.append("2.2 合同签订之日为实际签署之日，自双方代表签仪式立即生效。\n\n");

        content.append("第三条  费用与支付\n");
        content.append("3.1 乙方应向甲方支付费用总额为：¥").append(amount).append("元（大写：")
                .append(convertToChineseCurrency(amount)).append("元整）\n");
        content.append("3.2 乙方应在以下时间节点完成付款：\n");
        content.append("  - 签订合同时：支付总费用的30%，计¥").append(Math.round(amount * 30 / 100)).append("元\n");
        content.append("  - 服务交付完成时：支付剩余70%，计¥").append(Math.round(amount * 70 / 100)).append("元\n");
        content.append("3.3 付款方式：银行转账至甲方指定账户\n");
        content.append("3.4 如乙方逾期支付，应按日利率0.05‰支付逾期利息\n\n");

        content.append("第四条  双方权利与义务\n");
        content.append("4.1 甲方的权利与义务：\n");
        content.append("  (1) 有权审查乙方提供的资料完整性\n");
        content.append("  (2) 有义务严格履行合同义务，确保服务质量\n");
        content.append("  (3) 有义务按时完成服务工作\n\n");
        content.append("4.2 乙方的权利与义务：\n");
        content.append("  (1) 有权对甲方的服务质量进行监督\n");
        content.append("  (2) 有义务按时支付相应费用\n");
        content.append("  (3) 有义务提供必要的协作信息和资源\n\n");

        content.append("第五条  保密条款\n");
        content.append("5.1 双方对在履行本合同过程中获知的商业秘密承诺保密\n");
        content.append("5.2 保密义务在本合同终止后继续有效，期限为5年\n");
        content.append("5.3 因不可抗力或法律强制要求披露除外\n\n");

        content.append("第六条  违约责任\n");
        content.append("6.1 甲方逾期完成服务，每逾期一天按合同总价的0.1%支付违约金，逾期超30天乙方有权单方解除合同\n");
        content.append("6.2 乙方逾期支付费用，每逾期一天按未付金额的0.1%支付违约金\n");
        content.append("6.3 任何一方因违反合同条款给对方造成损失的，应赔偿对方的实际损失\n\n");

        content.append("第七条  争议解决\n");
        content.append("7.1 本合同的履行、解释和争议解决均受中华人民共和国法律管辖\n");
        content.append("7.2 双方发生争议时，首先应进行友好协商解决\n");
        content.append("7.3 协商不成的，双方同意提交至合同签订地人民法院诉讼解决\n\n");

        content.append("第八条  其他条款\n");
        content.append("8.1 本合同一式两份，甲乙双方各持一份，具有同等法律效力\n");
        content.append("8.2 未经双方书面协议，本合同不得以任何方式修改或变更\n");
        content.append("8.3 本合同条款如有违反法律规定，该条款无效，但不影响其他条款的效力\n\n");

        content.append("甲方（服务提供商）：_____________　　　　乙方（服务接收方）：_____________\n");
        content.append("签署日期：_____________　　　　　　　　签署日期：_____________\n");

        return content.toString();
    }

    private String convertToChineseCurrency(Double amount) {
        if (amount == null || amount == 0) return "零";

        long intPart = Math.round(amount);
        String[] units = {"", "十", "百", "千", "万", "十", "百", "千", "亿"};
        String[] digits = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};

        String intStr = String.valueOf(intPart);
        StringBuilder result = new StringBuilder();
        int len = intStr.length();

        for (int i = 0; i < len; i++) {
            int digit = Character.getNumericValue(intStr.charAt(i));
            int unitIndex = len - i - 1;

            if (digit == 0) {
                if (unitIndex > 0 && unitIndex % 4 == 0) {
                    result.append("万");
                }
                continue;
            }

            if (unitIndex % 4 == 0 && unitIndex > 0) {
                result.append("万");
            }

            result.append(digits[digit]);
            if (unitIndex % 4 > 0) {
                result.append(units[unitIndex % 4]);
            }
        }

        return result.toString();
    }

    private static final String DRAFT_SYSTEM_PROMPT = """
你是一名拥有15年执业经验的中国律师，擅长合同起草。
你必须生成符合中文法律文书规范的完整合同正文。
合同应包含标准的法律条款，表述专业、准确、严谨。
不要输出 JSON，而是输出完整的合同文本。
""";

    private static final String CITATION_PROTOCOL = """
【强制引用协议（必须遵守）】
- 你输出的 JSON 中，任何字符串数组的每一项、以及总结性字符串字段，
  其末尾都必须以方括号引用形式收尾，且引用必须来自用户给出的 RAG 编号集合：
    法条 → [L#]，类案 → [C#]，知识库 → [K#]
- 若某项确实没有可对应的引用，必须显式追加 "[N/A]" 占位。
- 一句话内可有多个引用，例如 "...建议补足合同主体条款[L1][K2]"。
- 严禁编造未在用户上下文中出现过的编号（例如用户只给了 L1~L5，就不能写 [L7]）。
- 上述规范不可省略，违反即视为错误输出。
""";

    private static final String CONSULT_SYSTEM_PROMPT = """
你是一名拥有15年执业经验的中国律师，擅长劳动法、合同法、侵权责任法。
你必须严格按照用户要求输出 JSON，且 JSON 可被解析（不要输出多余文本，不要使用 Markdown 围栏）。
""" + CITATION_PROTOCOL;

    private static final String CASE_ANALYSIS_SYSTEM_PROMPT = """
你是一名专业法律分析师，擅长梳理案件事实、识别争议焦点与证据缺口。
你必须严格按照用户要求输出 JSON，且 JSON 可被解析（不要输出多余文本，不要使用 Markdown 围栏）。
""" + CITATION_PROTOCOL;

    private static final String CONTRACT_REVIEW_SYSTEM_PROMPT = """
你是一名专业合同审查律师，能精准识别合同风险条款、缺失条款与不平衡条款。
你必须严格按照用户要求输出 JSON，且 JSON 可被解析（不要输出多余文本，不要使用 Markdown 围栏）。
""" + CITATION_PROTOCOL;

    private static final String CONTRACT_REVIEW_SELF_CHECK_SYSTEM_PROMPT = """
你是一名极其严谨的合同审查首席审核官，专门复核同事给出的初步合同审查结论。
你的任务是：基于原始合同内容、检索到的法条/知识库、以及同事的初稿 JSON，
进行二次校验：剔除幻觉、补齐遗漏、调整 confidence、保证每条 risk/missingClause 都能在合同正文或法条中找到依据，
并保证所有数组项末尾都带有正确的 [L#]/[K#]/[N/A] 引用收尾。
你必须严格按照用户要求输出 JSON，且 JSON 可被解析（不要输出多余文本，不要使用 Markdown 围栏）。
""" + CITATION_PROTOCOL;

    private String buildConsultUserPrompt(
            String question,
            List<String> facts,
            List<String> lawItems,
            List<String> caseItems,
            List<String> kbItems
    ) {
        return """
# 用户问题：
%s

# 相关事实（可为空）：
%s

# 检索到的法条（编号 L1, L2 ...，可为空）：
%s

# 检索到的类案（编号 C1, C2 ...，可为空）：
%s

# 本地知识库片段（编号 K1, K2 ...，可为空）：
%s

# 强制引用规范（极其重要，违反即视为错误输出）：
- legalBasis / recommendations / riskAlerts 中**每一项**都必须在末尾追加至少一个引用标记，
  来源于上面给出的 L#/C#/K# 编号集合，例如：
    "《劳动合同法》第10条……[L1]"
    "建议补充工资条作为证据…………[K2]"
    "类案中法院支持双倍工资请求……[C3]"
- 编号必须与上面提供的实际编号一致，禁止编造（如没有 L7 就不能写 [L7]）。
- 若实在没有可引用的检索项，请使用 "[N/A]" 显式占位（不要省略）。

# 输出要求：
只输出 JSON，不要输出任何解释性文字，不要使用 Markdown 围栏。
JSON 结构：
{
  "category": "string",
  "answer": "string",
  "legalBasis": ["string"],
  "recommendations": ["string"],
  "riskAlerts": ["string"],
  "confidence": 0.0
}

字段说明：
- answer 是面向用户的"自然语言主回答"（120~280 字），用第二人称，
  直接回答用户的问题、说清结论与最关键 1~2 个理由，可以在末尾带 [L#]/[C#]/[K#] 引用。
- legalBasis 每一项必须包含：法规名称 + 条款 + 内容摘要 + 末尾追加 [L#] / [K#]。
- recommendations 每一项给出可执行的下一步动作；末尾追加 [L#][C#][K#] 之一。
- riskAlerts 每一项是 1 条风险提示；末尾追加 [L#][C#][K#] 之一（无可引用就 [N/A]）。
- confidence 为 0.0~1.0 的浮点数，表示你对本次回答整体的可信度。
- 严禁编造实际不存在的法条/案例编号。

# 引用格式正确示例（仅作为格式示例，不要照抄内容）：
{
  "category": "劳动争议",
  "answer": "你已工作满 6 个月但公司未签订书面劳动合同，依《劳动合同法》第 82 条，自第二个月起公司应当向你支付二倍工资，建议先固定工资流水/聊天记录等证据再申请劳动仲裁[L1][L2]。",
  "legalBasis": [
    "《劳动合同法》第十条规定建立劳动关系应当订立书面劳动合同[L1]",
    "《劳动合同法》第八十二条规定未签书面合同应支付二倍工资[L2]"
  ],
  "recommendations": [
    "立即固定微信聊天记录、考勤、工资流水等证据[K1]",
    "向劳动监察大队投诉或申请劳动仲裁[L1][C1]"
  ],
  "riskAlerts": [
    "二倍工资请求权适用1年时效，注意时效抗辩风险[L2]"
  ],
  "confidence": 0.85
}
（注意每个数组项末尾都带 [L#]/[C#]/[K#]，无可引用时用 [N/A]）
""".formatted(
                question,
                facts.isEmpty() ? "[] " : facts,
                formatRagItems(lawItems, "L"),
                formatRagItems(caseItems, "C"),
                formatRagItems(kbItems, "K")
        );
    }

    private String buildCaseAnalysisUserPrompt(
            String caseSummary,
            List<String> evidencePoints,
            List<String> caseItems,
            List<String> kbItems
    ) {
        return """
# 案情摘要：
%s

# 证据要点：
%s

# 检索到的类案（编号 C1, C2 ...，可为空）：
%s

# 本地知识库片段（编号 K1, K2 ...，可为空）：
%s

# 强制引用规范（极其重要，违反即视为错误输出）：
- keyFacts / disputeFocalPoints / evidenceGaps / actionRecommendations 中**每一项**
  都必须在末尾追加至少一个引用标记 [C#] 或 [K#]（来源于上面提供的 C/K 编号集合）。
- 没有可引用的检索项时，请显式追加 "[N/A]"。
- 严禁编造不存在的编号（如没有 C9 就不能写 [C9]）。

# 输出要求：
只输出 JSON，不要输出任何解释性文字，不要使用 Markdown 围栏。
JSON 结构（字段名必须严格一致，不要改名）：
{
  "keyFacts": ["string"],
  "disputeFocalPoints": ["string"],
  "evidenceGaps": ["string"],
  "actionRecommendations": ["string"],
  "confidence": 0.0
}

字段说明：
- 数组内每项为简短要点 + 末尾 [C#]/[K#]/[N/A]。
- confidence 为 0.0~1.0 的浮点数，表示对当前案件分析的可信度。
""".formatted(
                caseSummary,
                evidencePoints.isEmpty() ? "[] " : evidencePoints,
                formatRagItems(caseItems, "C"),
                formatRagItems(kbItems, "K")
        );
    }

    private String buildContractReviewUserPrompt(
            String contractTitle,
            String limitedContent,
            List<String> lawItems,
            List<String> kbItems
    ) {
        return """
# 合同标题：
%s

# 合同正文（可能已截断）：
%s

# 检索到的法条（编号 L1, L2 ...，可为空）：
%s

# 本地知识库片段（编号 K1, K2 ...，可为空）：
%s

# 强制引用规范（极其重要，违反即视为错误输出）：
- 每条 risk.issue / risk.suggestion / missingClauses 项末尾**必须**追加至少一个
  来源于上面 L/K 编号集合的引用标记，例如 "...[L1]" 或 "...[K2]"。
- 没有可引用的检索项时，必须显式追加 "[N/A]"。
- 严禁编造不存在的编号（如没有 L9 就不能写 [L9]）。

# 输出要求：
只输出 JSON，不要输出任何解释性文字，不要使用 Markdown 围栏。
JSON 结构：
{
  "risks": [
    {
      "level": "LOW|MEDIUM|HIGH",
      "clauseTitle": "string",
      "issue": "string",
      "suggestion": "string"
    }
  ],
  "missingClauses": ["string"],
  "notes": "string",
  "confidence": 0.0
}

要求：
- risks 必须至少包含 1 条；missingClauses 必须至少包含 1 条（禁止空数组）。
- notes 用于总结审查结论与下一步建议（末尾也追加 [L#]/[K#]/[N/A]）。
- confidence 为 0.0~1.0 的浮点数。
- 严禁编造合同中实际不存在的条款；推断请在 issue 末尾追加 "[推断]"。

# 引用格式正确示例（仅作为格式示例，不要照抄内容）：
{
  "risks": [
    {
      "level": "HIGH",
      "clauseTitle": "单方解释权",
      "issue": "本合同最终解释权归甲方所有，违反公平原则[L1]",
      "suggestion": "改为双方协商解释或按法律规定处理[K1]"
    }
  ],
  "missingClauses": [
    "缺少违约责任条款[L2]",
    "缺少争议解决条款[N/A]"
  ],
  "notes": "已识别 1 项高风险条款，2 项关键缺失条款，建议补充并复核[L1][K1]",
  "confidence": 0.78
}
（注意每个数组项末尾都带 [L#]/[K#]/[N/A]）
""".formatted(
                contractTitle,
                limitedContent,
                formatRagItems(lawItems, "L"),
                formatRagItems(kbItems, "K")
        );
    }

    private String buildSelfCheckUserPrompt(
            String contractTitle,
            String limitedContent,
            List<String> lawItems,
            List<String> kbItems,
            String firstRoundJson
    ) {
        return """
# 合同标题：
%s

# 合同正文（可能已截断）：
%s

# 检索到的法条（编号 L1, L2 ...）：
%s

# 本地知识库片段（编号 K1, K2 ...）：
%s

# 同事第一轮审查 JSON（请你复核）：
%s

# 复核要求：
请按下列步骤思考（不要输出思考过程）：
1) 逐条检查 risks：合同正文是否真存在该条款或缺失？若纯属猜测，请删除该条或调整为 LOW 并加 "[推断]" 标注。
2) 检查 missingClauses：是否真的缺失？已存在的项要剔除。
3) 检查每项末尾的 [L#][K#] 引用编号是否真的对应检索结果，错的纠正、缺失的补上。
4) 根据复核结果调整 confidence；若大量改动，confidence 应下调。
5) 在 notes 中追加一句：“已完成二轮自检”。

输出与原 JSON 完全相同的结构（risks / missingClauses / notes / confidence），不要输出任何额外文本，不要使用 Markdown 围栏。
""".formatted(
                contractTitle,
                limitedContent,
                formatRagItems(lawItems, "L"),
                formatRagItems(kbItems, "K"),
                firstRoundJson
        );
    }

    /**
     * 把 RAG 检索结果格式化为带编号的多行文本：
     *   L1) 《劳动合同法》第10条 ...
     *   L2) 《劳动法》第50条 ...
     * 让大模型可以稳定按编号引用。
     */
    private static String formatRagItems(List<String> items, String prefix) {
        if (items == null || items.isEmpty()) return "[] (无检索结果)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            String raw = items.get(i) == null ? "" : items.get(i).trim();
            if (raw.length() > 320) raw = raw.substring(0, 320) + "...";
            sb.append(prefix).append(i + 1).append(") ").append(raw).append('\n');
        }
        return sb.toString();
    }

    private Optional<ConsultationResponse> tryParseConsultationResponse(String llmText) {
        try {
            String jsonText = extractJsonObject(llmText);
            JsonNode root = objectMapper.readTree(jsonText);
            String category = asText(root, "category");
            List<String> legalBasis = asTextArray(root, "legalBasis");
            List<String> recommendations = asTextArray(root, "recommendations");
            List<String> riskAlerts = asTextArray(root, "riskAlerts");
            Double confidence = asDouble(root, "confidence");

            if (category == null) return Optional.empty();
            String answer = asText(root, "answer");
            return Optional.of(new ConsultationResponse(
                    category,
                    legalBasis.isEmpty() ? List.of() : legalBasis,
                    recommendations.isEmpty() ? List.of() : recommendations,
                    riskAlerts.isEmpty() ? List.of() : riskAlerts,
                    confidence,
                    buildRetrievalContext(List.of(), List.of(), List.of()),
                    answer
            ));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    private Optional<CaseAnalysisResponse> tryParseCaseAnalysisResponse(String llmText) {
        try {
            String jsonText = extractJsonObject(llmText);
            JsonNode root = objectMapper.readTree(jsonText);
            // 允许模型输出字段名轻微偏差，尽量避免解析失败回兜底。
            List<String> keyFacts = asTextArray(root, "keyFacts");
            if (keyFacts.isEmpty()) keyFacts = asTextArray(root, "keyFactsList");

            List<String> disputeFocalPoints = asTextArray(root, "disputeFocalPoints");
            if (disputeFocalPoints.isEmpty()) disputeFocalPoints = asTextArray(root, "disputedIssues");
            if (disputeFocalPoints.isEmpty()) disputeFocalPoints = asTextArray(root, "disputeFocalPoint");

            List<String> evidenceGaps = asTextArray(root, "evidenceGaps");
            if (evidenceGaps.isEmpty()) evidenceGaps = asTextArray(root, "evidenceGap");

            List<String> actionRecommendations = asTextArray(root, "actionRecommendations");
            if (actionRecommendations.isEmpty()) actionRecommendations = asTextArray(root, "suggestedActions");
            if (actionRecommendations.isEmpty()) actionRecommendations = asTextArray(root, "actionRecommendation");

            Double confidence = asDouble(root, "confidence");

            if (keyFacts.isEmpty() && disputeFocalPoints.isEmpty() && evidenceGaps.isEmpty() && actionRecommendations.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new CaseAnalysisResponse(
                    keyFacts,
                    disputeFocalPoints,
                    evidenceGaps,
                    actionRecommendations,
                    confidence,
                    buildRetrievalContext(List.of(), List.of(), List.of())
            ));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    private Optional<ContractReviewResponse> tryParseContractReviewResponse(String llmText) {
        try {
            String jsonText = extractJsonObject(llmText);
            JsonNode root = objectMapper.readTree(jsonText);
            List<String> missingClauses = asTextArray(root, "missingClauses");
            String notes = asText(root, "notes");
            if (notes == null) notes = asText(root, "summary");
            Double confidence = asDouble(root, "confidence");

            List<ContractRiskItem> risks = new ArrayList<>();
            JsonNode risksNode = root.get("risks");
            if (risksNode != null && risksNode.isArray()) {
                for (JsonNode risk : risksNode) {
                    String levelText = asText(risk, "level");
                    RiskLevel level = parseRiskLevel(levelText).orElse(RiskLevel.LOW);
                    String clauseTitle = asText(risk, "clauseTitle");
                    if (clauseTitle == null) clauseTitle = asText(risk, "clause");
                    String issue = asText(risk, "issue");
                    String suggestion = asText(risk, "suggestion");
                    if (clauseTitle == null) clauseTitle = "合同风险项";
                    risks.add(new ContractRiskItem(level, clauseTitle, issue == null ? "" : issue, suggestion == null ? "" : suggestion));
                }
            }

            // 若模型给出的 missingClauses 为空，用 risks 的条款名兜底，避免前端缺字段。
            if (missingClauses.isEmpty() && !risks.isEmpty()) {
                List<String> derived = new ArrayList<>();
                for (ContractRiskItem item : risks) {
                    if (item.clause() != null && !item.clause().isBlank()) {
                        derived.add(item.clause());
                    }
                }
                if (!derived.isEmpty()) missingClauses = derived;
            }

            if (risks.isEmpty()) return Optional.empty();
            return Optional.of(new ContractReviewResponse(
                    risks,
                    missingClauses.isEmpty() ? List.of("未明确识别出具体缺失条款；建议结合合同类型补齐关键条款") : missingClauses,
                    notes == null ? "" : notes,
                    confidence,
                    buildRetrievalContext(List.of(), List.of(), List.of())
            ));
        } catch (Exception ignore) {
            return Optional.empty();
        }
    }

    private static RetrievalContext buildRetrievalContext(
            List<String> laws,
            List<String> cases,
            List<String> knowledge
    ) {
        return new RetrievalContext(
                laws == null ? List.of() : laws,
                cases == null ? List.of() : cases,
                knowledge == null ? List.of() : knowledge
        );
    }

    private static ConsultationResponse withRetrievalContext(
            ConsultationResponse response,
            List<String> laws,
            List<String> cases,
            List<String> knowledge
    ) {
        return new ConsultationResponse(
                response.category(),
                response.legalBasis(),
                response.recommendations(),
                response.riskAlerts(),
                response.confidence(),
                buildRetrievalContext(laws, cases, knowledge),
                response.answer()
        );
    }

    private static CaseAnalysisResponse withRetrievalContext(
            CaseAnalysisResponse response,
            List<String> laws,
            List<String> cases,
            List<String> knowledge
    ) {
        return new CaseAnalysisResponse(
                response.keyFacts(),
                response.disputedIssues(),
                response.evidenceGaps(),
                response.suggestedActions(),
                response.confidence(),
                buildRetrievalContext(laws, cases, knowledge)
        );
    }

    private static ContractReviewResponse withRetrievalContext(
            ContractReviewResponse response,
            List<String> laws,
            List<String> cases,
            List<String> knowledge
    ) {
        return new ContractReviewResponse(
                response.risks(),
                response.missingClauses(),
                response.summary(),
                response.confidence(),
                buildRetrievalContext(laws, cases, knowledge)
        );
    }

    private static Optional<RiskLevel> parseRiskLevel(String levelText) {
        if (levelText == null) return Optional.empty();
        String normalized = levelText.trim().toUpperCase(Locale.ROOT);
        for (RiskLevel v : RiskLevel.values()) {
            if (v.name().equals(normalized)) return Optional.of(v);
        }
        return Optional.empty();
    }

    private static String extractJsonObject(String text) {
        // 支持：模型可能输出 ```json ... ``` 或直接输出 { ... }
        String t = text.trim();
        int firstBrace = t.indexOf('{');
        int lastBrace = t.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw new IllegalArgumentException("no json object found");
        }
        return t.substring(firstBrace, lastBrace + 1);
    }

    private String inferConsultationCategory(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (q.contains("劳动") || q.contains("工资") || q.contains("社保")) return "劳动争议";
        if (q.contains("合同") || q.contains("违约")) return "合同纠纷";
        if (q.contains("离婚") || q.contains("抚养")) return "婚姻家事";
        if (q.contains("侵权") || q.contains("赔偿")) return "侵权责任";
        return "综合法律咨询";
    }

    private static String asText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isTextual()) return v.asText();
        return v.toString();
    }

    private static List<String> asTextArray(JsonNode node, String field) {
        if (node == null) return List.of();
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return List.of();
        if (!v.isArray()) return List.of(v.asText());
        List<String> out = new ArrayList<>();
        for (JsonNode item : v) {
            if (item == null || item.isNull()) continue;
            if (item.isTextual()) out.add(item.asText());
            else out.add(item.toString());
        }
        return out;
    }

    private static Double asDouble(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) return null;
        if (v.isNumber()) return v.asDouble();
        try {
            return Double.parseDouble(v.asText().trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private static <T> T safeCall(SupplierEx<T> supplier, T defaultValue) {
        try {
            T v = supplier.get();
            return v == null ? defaultValue : v;
        } catch (Exception ignore) {
            return defaultValue;
        }
    }

    @FunctionalInterface
    private interface SupplierEx<T> {
        T get();
    }
}

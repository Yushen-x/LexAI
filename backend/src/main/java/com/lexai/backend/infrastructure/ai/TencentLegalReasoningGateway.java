package com.lexai.backend.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.ContractRiskItem;
import com.lexai.backend.application.port.out.LegalReasoningGateway;
import com.lexai.backend.domain.model.RiskLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 腾讯大模型 + 得理开放平台检索（可选本地知识库）的法律智能网关。
 *
 * 说明：
 * 1) 当未配置腾讯大模型 endpoint 或调用失败时，会自动退化到“模板输出 + 检索结果”策略，保证系统可用。
 * 2) 得理接口解析字段可能因接口返回结构差异而失败，因此也做了多种兜底解析。
 */
@Component
@ConditionalOnProperty(name = "lexai.ai.mode", havingValue = "tencent")
public class TencentLegalReasoningGateway implements LegalReasoningGateway {

    private final DeliCaseSearchClient deliCaseSearchClient;
    private final DeliLegalSearchClient deliLegalSearchClient;
    private final LocalKnowledgeSearchClient localKnowledgeSearchClient;
    private final TencentLLMClient tencentLLMClient;
    private final ObjectMapper objectMapper;

    public TencentLegalReasoningGateway(
            DeliCaseSearchClient deliCaseSearchClient,
            DeliLegalSearchClient deliLegalSearchClient,
            LocalKnowledgeSearchClient localKnowledgeSearchClient,
            TencentLLMClient tencentLLMClient,
            ObjectMapper objectMapper
    ) {
        this.deliCaseSearchClient = deliCaseSearchClient;
        this.deliLegalSearchClient = deliLegalSearchClient;
        this.localKnowledgeSearchClient = localKnowledgeSearchClient;
        this.tencentLLMClient = tencentLLMClient;
        this.objectMapper = objectMapper;
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
                return parsed.get();
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

        return new ConsultationResponse(category, legalBasis, recommendations, riskAlerts);
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
                return parsed.get();
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

        return new CaseAnalysisResponse(keyFacts, disputedIssues, evidenceGaps, suggestedActions);
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

        String llmText = tencentLLMClient.chat(systemPrompt, userPrompt);
        if (llmText != null) {
            Optional<ContractReviewResponse> parsed = tryParseContractReviewResponse(llmText);
            if (parsed.isPresent()) {
                return parsed.get();
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
                "当前审查结果为“可用兜底”策略：已接入得理检索/本地知识库的上下文能力，但腾讯大模型调用未配置或解析失败时将使用规则化分析。"
        );
    }

    private static final String CONSULT_SYSTEM_PROMPT = """
你是一名拥有15年执业经验的中国律师，擅长劳动法、合同法、侵权责任法。
你必须严格按照用户要求输出 JSON，且 JSON 可被解析（不要输出多余文本）。
""";

    private static final String CASE_ANALYSIS_SYSTEM_PROMPT = """
你是一名专业法律分析师，擅长梳理案件事实、识别争议焦点与证据缺口。
你必须严格按照用户要求输出 JSON，且 JSON 可被解析（不要输出多余文本）。
""";

    private static final String CONTRACT_REVIEW_SYSTEM_PROMPT = """
你是一名专业合同审查律师，能精准识别合同风险条款、缺失条款与不平衡条款。
你必须严格按照用户要求输出 JSON，且 JSON 可被解析（不要输出多余文本）。
""";

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

# 检索到的法条（可为空）：
%s

# 检索到的类案（可为空）：
%s

# 本地知识库片段（可为空）：
%s

# 输出要求：
只输出 JSON，不要输出任何解释性文字。
JSON 结构：
{
  "category": "string",
  "legalBasis": ["string"],
  "recommendations": ["string"],
  "riskAlerts": ["string"]
}

legalBasis 建议包含：法规名称 + 条款/条文 + 内容摘要。
recommendations 建议包含：下一步行动建议。
riskAlerts 建议包含：风险提示（时效/证据/适用条件等）。
""".formatted(
                question,
                facts.isEmpty() ? "[] " : facts,
                lawItems.isEmpty() ? "[] " : lawItems,
                caseItems.isEmpty() ? "[] " : caseItems,
                kbItems.isEmpty() ? "[] " : kbItems
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

# 检索到的类案（可为空）：
%s

# 本地知识库片段（可为空）：
%s

# 输出要求：
只输出 JSON，不要输出任何解释性文字。
JSON 结构（字段名必须严格一致，不要改名）：
{
  "keyFacts": ["string"],
  "disputeFocalPoints": ["string"],
  "evidenceGaps": ["string"],
  "actionRecommendations": ["string"]
}

注意：不要输出除 JSON 外的任何内容；数组内每项为简短要点。
""".formatted(
                caseSummary,
                evidencePoints.isEmpty() ? "[] " : evidencePoints,
                caseItems.isEmpty() ? "[] " : caseItems,
                kbItems.isEmpty() ? "[] " : kbItems
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

# 检索到的法条（可为空）：
%s

# 本地知识库片段（可为空）：
%s

# 输出要求：
只输出 JSON，不要输出任何解释性文字。
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
  "notes": "string"
}

risks 必须至少包含 1 条；missingClauses 必须至少包含 1 条（禁止输出空数组，无法判断就给最核心缺失项描述）。
notes 用于总结审查结论与下一步建议。
""".formatted(
                contractTitle,
                limitedContent,
                lawItems.isEmpty() ? "[] " : lawItems,
                kbItems.isEmpty() ? "[] " : kbItems
        );
    }

    private Optional<ConsultationResponse> tryParseConsultationResponse(String llmText) {
        try {
            String jsonText = extractJsonObject(llmText);
            JsonNode root = objectMapper.readTree(jsonText);
            String category = asText(root, "category");
            List<String> legalBasis = asTextArray(root, "legalBasis");
            List<String> recommendations = asTextArray(root, "recommendations");
            List<String> riskAlerts = asTextArray(root, "riskAlerts");

            if (category == null) return Optional.empty();
            return Optional.of(new ConsultationResponse(
                    category,
                    legalBasis.isEmpty() ? List.of() : legalBasis,
                    recommendations.isEmpty() ? List.of() : recommendations,
                    riskAlerts.isEmpty() ? List.of() : riskAlerts
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

            if (keyFacts.isEmpty() && disputeFocalPoints.isEmpty() && evidenceGaps.isEmpty() && actionRecommendations.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new CaseAnalysisResponse(
                    keyFacts,
                    disputeFocalPoints,
                    evidenceGaps,
                    actionRecommendations
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
                    notes == null ? "" : notes
            ));
        } catch (Exception ignore) {
            return Optional.empty();
        }
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


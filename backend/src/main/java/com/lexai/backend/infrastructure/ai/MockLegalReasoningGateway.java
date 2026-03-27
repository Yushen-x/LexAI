package com.lexai.backend.infrastructure.ai;

import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractDraftRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractDraftResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.ContractRiskItem;
import com.lexai.backend.application.port.out.LegalReasoningGateway;
import com.lexai.backend.domain.model.RiskLevel;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class MockLegalReasoningGateway implements LegalReasoningGateway {

    @Override
    public ConsultationResponse consult(ConsultationRequest request) {
        String question = request.question().toLowerCase(Locale.ROOT);
        String category = inferConsultationCategory(question);

        return new ConsultationResponse(
                category,
                List.of(
                        "建议结合对应法律法规、司法解释及事实证据进行综合判断",
                        "可进一步接入法律知识库输出更精确的条文依据"
                ),
                List.of(
                        "先补充关键信息并固定证据",
                        "根据争议类型选择协商、投诉、仲裁或诉讼路径",
                        "必要时提交给人工律师复核"
                ),
                List.of(
                        "当前结果为 AI 辅助分析，不替代正式法律意见",
                        "若涉及金额较大或时效风险，建议尽快人工介入"
                )
        );
    }

    @Override
    public CaseAnalysisResponse analyzeCase(CaseAnalysisRequest request) {
        List<String> evidencePoints = request.evidencePoints() == null ? List.of() : request.evidencePoints();

        return new CaseAnalysisResponse(
                List.of(
                        "已识别案件核心叙事与主要争议背景",
                        "可提取双方主体关系、履约过程与争议节点",
                        "当前输入长度适合进入争议焦点细分阶段"
                ),
                List.of(
                        "责任承担边界是否明确",
                        "现有证据是否足以支撑核心主张",
                        "是否存在先履行抗辩、违约抗辩或程序性争议"
                ),
                evidencePoints.isEmpty()
                        ? List.of("尚未录入具体证据，建议补充合同、聊天记录、付款凭证等")
                        : List.of("建议核验时间线完整性", "建议补充能直接证明违约或损失的关键证据"),
                List.of(
                        "整理时间线并建立证据目录",
                        "围绕争议焦点逐项匹配证据材料",
                        "形成诉讼或谈判版本的事实摘要"
                )
        );
    }

    @Override
    public ContractReviewResponse reviewContract(ContractReviewRequest request) {
        String content = request.contractContent().toLowerCase(Locale.ROOT);
        List<ContractRiskItem> risks = new ArrayList<>();
        List<String> missingClauses = new ArrayList<>();

        if (!content.contains("违约")) {
            missingClauses.add("违约责任条款");
        }
        if (!content.contains("保密")) {
            missingClauses.add("保密条款");
        }
        if (!content.contains("解除")) {
            missingClauses.add("解除与终止条款");
        }
        if (!content.contains("争议解决")) {
            missingClauses.add("争议解决条款");
        }

        if (content.contains("单方解释权归甲方所有")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.HIGH,
                    "单方解释权条款",
                    "条款存在明显失衡，可能引发显失公平或无效风险",
                    "删除单方绝对解释权表述，改为双方协商解释或按法律规定处理"
            ));
        }

        if (!content.contains("付款")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.MEDIUM,
                    "付款安排",
                    "缺少付款节点、金额及逾期处理机制，履约争议风险较高",
                    "补充付款条件、付款期限、发票规则及逾期责任"
            ));
        }

        if (!content.contains("交付") && !content.contains("验收")) {
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
                missingClauses,
                "当前审查结果基于规则化 Mock 引擎，后续可升级为腾讯 AI + 法律知识库 + 风险评分模型。"
        );
    }

    @Override
    public ContractDraftResponse draftContract(ContractDraftRequest request) {
        StringBuilder content = new StringBuilder();
        content.append(request.contractName()).append("\n\n");
        content.append("本合同由以下各方于")
                .append(java.time.LocalDate.now())
                .append("签订：\n\n");

        content.append("甲方（服务提供商）：").append(request.partyA()).append("\n");
        content.append("乙方（服务接收方）：").append(request.partyB()).append("\n\n");

        content.append("鉴于：\n");
        content.append("甲方同意为乙方提供相关服务，乙方同意支付相应费用。双方在平等、自愿、协商一致的基础上，达成如下协议：\n\n");

        content.append("第一条  服务内容与范围\n");
        content.append("1.1 甲方向乙方提供相关的服务。具体服务内容为：")
                .append(request.requirements() != null ? request.requirements() : "详见双方另行商定的明细表")
                .append("\n");
        content.append("1.2 甲方承诺按照约定的时间、质量标准完成上述服务。\n\n");

        content.append("第二条  服务期限\n");
        content.append("2.1 合同服务期限为：").append(request.duration() != null ? request.duration() : "自签订之日起12个月")
                .append("\n");
        content.append("2.2 合同签订之日为实际签署之日，自双方代表签仪式立即生效。\n\n");

        content.append("第三条  费用与支付\n");
        content.append("3.1 乙方应向甲方支付费用总额为：¥").append(request.amount()).append("元（大写：")
                .append(convertToChineseCurrency(request.amount())).append("元整）\n");
        content.append("3.2 乙方应在以下时间节点完成付款：\n");
        content.append("  - 签订合同时：支付总费用的30%，计¥").append(request.amount() * 30 / 100).append("元\n");
        content.append("  - 服务交付完成时：支付剩余70%，计¥").append(request.amount() * 70 / 100).append("元\n");
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
        content.append("签署日期：_____________　　　　　　　　　签署日期：_____________\n");

        String summary = "已生成包含服务内容、费用安排、权利义务、违约责任、争议解决等完整条款的合同草稿。"
                + "建议甲方=\"" + request.partyA() + "\"和乙方=\"" + request.partyB() + "\"进行人工复核，"
                + "结合实际业务情况补充或修改相关条款。";

        return new ContractDraftResponse(
                content.toString(),
                summary,
                LocalDateTime.now()
        );
    }

    private String inferConsultationCategory(String question) {
        if (question.contains("劳动") || question.contains("工资") || question.contains("社保")) {
            return "劳动争议";
        }
        if (question.contains("合同") || question.contains("违约")) {
            return "合同纠纷";
        }
        if (question.contains("离婚") || question.contains("抚养")) {
            return "婚姻家事";
        }
        if (question.contains("侵权") || question.contains("赔偿")) {
            return "侵权责任";
        }
        return "综合法律咨询";
    }

    private String convertToChineseCurrency(Long amount) {
        // Simple conversion for demo purposes
        if (amount >= 100000000) {
            return (amount / 100000000) + "亿";
        } else if (amount >= 10000) {
            return (amount / 10000) + "万";
        } else if (amount >= 1000) {
            return (amount / 1000) + "千";
        }
        return amount.toString();
    }
}


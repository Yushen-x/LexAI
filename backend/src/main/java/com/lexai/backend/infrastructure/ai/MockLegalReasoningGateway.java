package com.lexai.backend.infrastructure.ai;

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
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "lexai.ai.mode", havingValue = "mock", matchIfMissing = true)
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
}


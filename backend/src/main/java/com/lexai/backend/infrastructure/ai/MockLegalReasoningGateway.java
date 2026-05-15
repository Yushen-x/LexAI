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
import com.lexai.backend.application.dto.response.RetrievalContext;
import com.lexai.backend.application.port.out.LegalReasoningGateway;
import com.lexai.backend.domain.model.RiskLevel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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
                        "建议结合《中华人民共和国民法典》与相关司法解释核验事实与权利义务",
                        "对于高风险争议，需结合证据链、时效和程序规则综合判断"
                ),
                List.of(
                        "先固定关键证据，再明确争议焦点与诉求",
                        "优先尝试协商或函告，再决定是否进入仲裁或诉讼",
                        "涉及金额较大或主体复杂时建议人工律师复核"
                ),
                List.of(
                        "当前结果为 AI 辅助分析，不替代正式法律意见",
                        "若存在时效、管辖或证据瑕疵，应尽快补强材料"
                ),
                0.6,
                new RetrievalContext(
                        List.of("《中华人民共和国民法典》相关条款", "《中华人民共和国劳动合同法》争议处理条款"),
                        List.of("最高人民法院相关类案裁判要旨（示例）"),
                        List.of("LexAI 内置知识库命中：争议处理与证据固定流程")
                ),
                "（Mock 模式回答）你的问题归类为「" + category + "」。建议先固定证据并明确诉求，再结合下方法律依据与风险提示决定是否进入仲裁/诉讼。"
        );
    }

    @Override
    public CaseAnalysisResponse analyzeCase(CaseAnalysisRequest request) {
        List<String> evidencePoints = request.evidencePoints() == null ? List.of() : request.evidencePoints();
        return new CaseAnalysisResponse(
                List.of(
                        "已识别案件主体关系、履约经过与争议背景",
                        "当前输入已具备继续拆解争议焦点的基础",
                        "建议围绕时间线与责任边界继续细化事实"
                ),
                List.of(
                        "现有证据是否足以支持核心主张",
                        "是否存在违约抗辩、先履行抗辩或程序性争议",
                        "损失范围与因果关系能否被有效证明"
                ),
                evidencePoints.isEmpty()
                        ? List.of("尚未录入具体证据，建议补充合同、聊天记录、付款凭证等材料")
                        : List.of("建议补强能直接证明违约或损失的核心证据", "建议核验时间线完整性与证据来源一致性"),
                List.of(
                        "整理案件时间线并建立证据目录",
                        "按争议焦点逐项匹配证据与法律依据",
                        "形成适合谈判、仲裁或诉讼的事实摘要版本"
                ),
                0.6,
                new RetrievalContext(
                        List.of(),
                        List.of("合同违约责任裁判类案（示例）", "劳动关系认定裁判类案（示例）"),
                        List.of("LexAI 证据审查知识片段：时间线与证据链补强")
                )
        );
    }

    @Override
    public ContractReviewResponse reviewContract(ContractReviewRequest request) {
        String content = request.contractContent().toLowerCase(Locale.ROOT);
        List<ContractRiskItem> risks = new ArrayList<>();
        List<String> missingClauses = new ArrayList<>();

        if (!content.contains("违约")) missingClauses.add("违约责任条款");
        if (!content.contains("保密")) missingClauses.add("保密条款");
        if (!content.contains("解除")) missingClauses.add("解除与终止条款");
        if (!content.contains("争议解决")) missingClauses.add("争议解决条款");

        if (content.contains("单方解释权")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.HIGH,
                    "单方解释权条款",
                    "条款失衡，存在显失公平或无效风险",
                    "建议删除单方绝对解释权表述，改为双方协商解释或依法处理"
            ));
        }
        if (!content.contains("付款")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.MEDIUM,
                    "付款安排",
                    "缺少付款节点、金额和逾期处理机制，履约争议风险较高",
                    "建议补充付款条件、付款期限、发票规则及逾期责任"
            ));
        }
        if (!content.contains("交付") && !content.contains("验收")) {
            risks.add(new ContractRiskItem(
                    RiskLevel.MEDIUM,
                    "交付与验收",
                    "缺少交付标准和验收机制，不利于界定履约完成时点",
                    "建议补充交付清单、验收标准、验收时间与异议处理规则"
            ));
        }
        if (risks.isEmpty()) {
            risks.add(new ContractRiskItem(
                    RiskLevel.LOW,
                    "整体结构",
                    "未发现显著高风险条款，但仍建议结合业务事实人工复核",
                    "建议继续核验主体信息、违约责任和争议解决条款"
            ));
        }

        return new ContractReviewResponse(
                risks,
                missingClauses,
                "当前审查结果基于 Mock 审查引擎生成，可用于演示合同风险识别与缺失条款提示。",
                0.6,
                new RetrievalContext(
                        List.of("《中华人民共和国民法典》合同编格式条款与违约责任条款"),
                        List.of(),
                        List.of("LexAI 合同风险知识库命中：付款、验收、争议解决、保密")
                )
        );
    }

    @Override
    public ContractDraftResponse draftContract(ContractDraftRequest request) {
        String requirements = request.requirements() == null || request.requirements().isBlank()
                ? "详见双方后续确认的业务清单。"
                : request.requirements().trim();
        String duration = request.duration() == null || request.duration().isBlank()
                ? "自签署之日起 12 个月"
                : request.duration().trim();

        String content = """
                %s

                甲方：%s
                乙方：%s

                第一条 合同目的
                双方基于平等、自愿、诚实信用原则，就%s相关事项达成一致。

                第二条 服务/合作内容
                1. 乙方按照甲方需求提供约定服务或交付成果。
                2. 核心需求如下：
                %s

                第三条 合同金额与支付
                1. 合同总金额为人民币 %d 元。
                2. 支付节点、发票要求与逾期责任由双方按本合同附件执行。

                第四条 履行期限
                合同履行期限为：%s。

                第五条 保密条款
                双方对履约过程中知悉的商业信息负有保密义务，未经对方书面同意不得向第三方披露。

                第六条 违约责任
                任一方违反合同义务造成对方损失的，应承担相应违约责任并赔偿实际损失。

                第七条 争议解决
                因本合同引起的争议，双方应先协商解决；协商不成的，提交有管辖权的人民法院处理。

                第八条 其他
                本合同自双方签字盖章之日起生效。

                签署日期：%s
                """.formatted(
                request.contractName(),
                request.partyA(),
                request.partyB(),
                request.contractType(),
                requirements,
                request.amount(),
                duration,
                LocalDate.now()
        );

        return new ContractDraftResponse(
                request.contractName(),
                content,
                "已生成包含金额、期限、保密、违约责任和争议解决条款的合同草稿，请结合实际业务继续补充细节。",
                LocalDateTime.now(),
                0.6,
                new RetrievalContext(
                        List.of("《中华人民共和国民法典》合同编通则相关条款"),
                        List.of(),
                        List.of("LexAI 合同模板知识库（Mock）")
                )
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

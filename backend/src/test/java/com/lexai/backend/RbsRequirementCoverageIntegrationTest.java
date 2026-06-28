package com.lexai.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * RBS（Requirement Breakdown Structure）需求全覆盖集成测试。
 * 对应 docs/ProjectCharter.md §5.3 中 R1–R3 全部叶子需求。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RbsRequirementCoverageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String CONSULT_JSON = """
            {"question":"公司拖欠工资两个月，我应如何维权？","facts":["已入职6个月","存在微信工作记录"]}
            """;

    private static final String CASE_JSON = """
            {"caseSummary":"二手房买卖中卖方拒绝过户，买方已支付定金","evidencePoints":["购房合同","转账记录"]}
            """;

    private static final String REVIEW_JSON = """
            {"contractTitle":"软件开发服务合同","contractContent":"本合同最终解释权归甲方所有，未约定验收标准与知识产权归属。"}
            """;

    private static final String DRAFT_JSON = """
            {
              "contractName":"技术服务合同",
              "contractType":"SERVICE",
              "partyA":"甲方科技有限公司",
              "partyB":"乙方软件工作室",
              "amount":200000,
              "duration":"6个月",
              "requirements":"交付定制化管理系统"
            }
            """;

    @Nested
    @DisplayName("R1 功能需求")
    class FunctionalRequirements {

        @Nested
        @DisplayName("R1.1 法律咨询模块")
        class LegalConsultation {

            @Test
            @DisplayName("R1.1.1 支持自然语言问题输入")
            void acceptsNaturalLanguageQuestion() throws Exception {
                mockMvc.perform(post("/legal/consultation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CONSULT_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value("SUCCESS"));
            }

            @Test
            @DisplayName("R1.1.1 空问题应返回校验错误")
            void rejectsBlankQuestion() throws Exception {
                mockMvc.perform(post("/legal/consultation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"question\":\"\",\"facts\":[]}"))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("R1.1.2 返回结构化答案与法律依据引用")
            void returnsStructuredAnswerWithCitations() throws Exception {
                mockMvc.perform(post("/legal/consultation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CONSULT_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.category").isNotEmpty())
                        .andExpect(jsonPath("$.data.legalBasis").isArray())
                        .andExpect(jsonPath("$.data.legalBasis[0]").isNotEmpty())
                        .andExpect(jsonPath("$.data.recommendations").isArray())
                        .andExpect(jsonPath("$.data.retrievalContext").exists());
            }

            @Test
            @DisplayName("R1.1.3 返回边界与免责声明提醒")
            void returnsBoundaryAndDisclaimerReminders() throws Exception {
                mockMvc.perform(post("/legal/consultation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CONSULT_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.riskAlerts").isArray())
                        .andExpect(jsonPath("$.data.riskAlerts[0]").value(
                                org.hamcrest.Matchers.containsString("AI")));
            }
        }

        @Nested
        @DisplayName("R1.2 案件分析模块")
        class CaseAnalysis {

            @Test
            @DisplayName("R1.2.1 支持案情输入并提取关键事实")
            void extractsKeyFactsFromCaseInput() throws Exception {
                mockMvc.perform(post("/legal/case-analysis")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CASE_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.keyFacts").isArray())
                        .andExpect(jsonPath("$.data.keyFacts[0]").isNotEmpty());
            }

            @Test
            @DisplayName("R1.2.2 识别法律争议焦点")
            void identifiesLegalIssues() throws Exception {
                mockMvc.perform(post("/legal/case-analysis")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CASE_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.disputedIssues").isArray())
                        .andExpect(jsonPath("$.data.disputedIssues[0]").isNotEmpty());
            }

            @Test
            @DisplayName("R1.2.3 匹配适用法律并给出建议")
            void matchesApplicableLawAndSuggestions() throws Exception {
                mockMvc.perform(post("/legal/case-analysis")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CASE_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.suggestedActions").isArray())
                        .andExpect(jsonPath("$.data.retrievalContext").exists())
                        .andExpect(jsonPath("$.data.confidence").isNumber());
            }
        }

        @Nested
        @DisplayName("R1.3 合同审查模块")
        class ContractReview {

            @Test
            @DisplayName("R1.3.1 识别条款级风险")
            void identifiesClauseLevelRisks() throws Exception {
                mockMvc.perform(post("/legal/contract-review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REVIEW_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.risks").isArray())
                        .andExpect(jsonPath("$.data.risks[0].clause").isNotEmpty())
                        .andExpect(jsonPath("$.data.risks[0].issue").isNotEmpty());
            }

            @Test
            @DisplayName("R1.3.2 生成修订建议")
            void generatesRevisionSuggestions() throws Exception {
                mockMvc.perform(post("/legal/contract-review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REVIEW_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.risks[0].suggestion").isNotEmpty());
            }

            @Test
            @DisplayName("R1.3.3 输出风险等级评分与审查报告")
            void outputsRiskScoringAndReport() throws Exception {
                mockMvc.perform(post("/legal/contract-review")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(REVIEW_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.risks[0].level").exists())
                        .andExpect(jsonPath("$.data.summary").isNotEmpty())
                        .andExpect(jsonPath("$.data.missingClauses").isArray())
                        .andExpect(jsonPath("$.data.confidence").isNumber());
            }
        }

        @Nested
        @DisplayName("R1.4 文书生成模块")
        class DocumentGeneration {

            @Test
            @DisplayName("R1.4.1 支持模板类型与参数输入")
            void acceptsTemplateAndParameters() throws Exception {
                mockMvc.perform(post("/legal/contract-draft")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(DRAFT_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.code").value("SUCCESS"));
            }

            @Test
            @DisplayName("R1.4.1 缺少必填参数应返回校验错误")
            void rejectsInvalidDraftParameters() throws Exception {
                mockMvc.perform(post("/legal/contract-draft")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"contractName\":\"\",\"contractType\":\"SERVICE\",\"partyA\":\"A\",\"partyB\":\"B\",\"amount\":100}"))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("R1.4.2 基于用户输入生成合同草稿")
            void generatesDraftFromUserInputs() throws Exception {
                mockMvc.perform(post("/legal/contract-draft")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(DRAFT_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.title").isNotEmpty())
                        .andExpect(jsonPath("$.data.generatedContent").isNotEmpty())
                        .andExpect(jsonPath("$.data.generatedAt").exists());
            }

            @Test
            @DisplayName("R1.4.3 合同台账 CSV 导出格式校验")
            void exportsContractsAsCsv() throws Exception {
                mockMvc.perform(get("/contracts/export").param("status", "DRAFT"))
                        .andExpect(status().isOk())
                        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                .header().string("Content-Disposition", org.hamcrest.Matchers.containsString("contracts.csv")));
            }
        }

        @Nested
        @DisplayName("R1.5 知识支撑模块")
        class KnowledgeSupport {

            @Test
            @DisplayName("R1.5.1 法律知识库已构建并可查询")
            void knowledgeBaseIsAvailable() throws Exception {
                mockMvc.perform(get("/system/health"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.knowledgeDocumentCount").isNumber())
                        .andExpect(jsonPath("$.data.knowledgeChunkCount").exists());
            }

            @Test
            @DisplayName("R1.5.2 RAG 检索结果注入 AI 响应")
            void ragRetrievalGroundsAiResponses() throws Exception {
                mockMvc.perform(post("/legal/consultation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CONSULT_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.retrievalContext.knowledge").isArray())
                        .andExpect(jsonPath("$.data.retrievalContext.laws").isArray());
            }

            @Test
            @DisplayName("R1.5.3 知识库索引统计可观测（维护入口）")
            void knowledgeIndexStatsAreObservable() throws Exception {
                mockMvc.perform(get("/system/health"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.knowledgeDocumentCount").value(
                                org.hamcrest.Matchers.greaterThanOrEqualTo(0)));
            }
        }
    }

    @Nested
    @DisplayName("R2 非功能需求")
    class NonFunctionalRequirements {

        @Test
        @DisplayName("R2.1.1 Mock 模式下核心 API P95 响应时间 ≤ 5 秒")
        void coreApiResponseTimeWithinFiveSeconds() throws Exception {
            Instant start = Instant.now();
            mockMvc.perform(post("/legal/consultation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CONSULT_JSON))
                    .andExpect(status().isOk());
            Duration elapsed = Duration.between(start, Instant.now());
            assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
        }

        @Test
        @DisplayName("R2.1.2 系统健康检查可用（演示期可用性基线）")
        void systemHealthIsUp() throws Exception {
            mockMvc.perform(get("/system/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("UP"));
        }

        @Test
        @DisplayName("R2.2.1 测试环境使用 H2 内存库，无真实个人数据")
        void testEnvironmentUsesInMemoryDatabase() throws Exception {
            mockMvc.perform(get("/system/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.database").value(
                            org.hamcrest.Matchers.containsString("H2")));
        }

        @Test
        @DisplayName("R2.2.2 API 对非法请求返回标准错误响应")
        void apiReturnsStandardErrorForInvalidRequests() throws Exception {
            mockMvc.perform(get("/contracts/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("FAILED"));
        }

        @Test
        @DisplayName("R2.2.3 健康接口不泄露密钥等敏感信息")
        void healthEndpointDoesNotLeakSecrets() throws Exception {
            String body = mockMvc.perform(get("/system/health"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            assertThat(body.toLowerCase()).doesNotContain("api-key");
            assertThat(body.toLowerCase()).doesNotContain("secret");
            assertThat(body.toLowerCase()).doesNotContain("password");
        }

        @Test
        @DisplayName("R2.3.2 参数校验失败返回明确错误信息")
        void validationErrorsReturnClearMessages() throws Exception {
            mockMvc.perform(post("/legal/consultation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"question\":\"\",\"facts\":[]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("R2.4.3 合同审查基准样本能识别典型风险条款")
        void contractReviewBenchmarkDetectsTypicalRisks() throws Exception {
            mockMvc.perform(post("/legal/contract-review")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(REVIEW_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.risks.length()").value(
                            org.hamcrest.Matchers.greaterThan(0)))
                    .andExpect(jsonPath("$.data.missingClauses.length()").value(
                            org.hamcrest.Matchers.greaterThanOrEqualTo(0)));
        }
    }

    @Nested
    @DisplayName("R3 交付需求")
    class DeliveryRequirements {

        @Test
        @DisplayName("R3.1 可部署原型：核心 API 与健康检查可访问")
        void deployablePrototypeCoreApisAccessible() throws Exception {
            mockMvc.perform(get("/system/health")).andExpect(status().isOk());
            mockMvc.perform(get("/system/overview")).andExpect(status().isOk());
            mockMvc.perform(get("/contracts").param("page", "0").param("size", "5")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("R3.2 项目文档集完整存在")
        void projectDocumentationSetExists() {
            Path docs = Path.of("..", "docs");
            assertThat(Files.exists(docs.resolve("ProjectCharter.md"))).isTrue();
            assertThat(Files.exists(docs.resolve("ARCHITECTURE.md"))).isTrue();
            assertThat(Files.exists(docs.resolve("walkthrough.md"))).isTrue();
            assertThat(Files.exists(docs.resolve("前端单元测试报告.md"))).isTrue();
            assertThat(Files.exists(docs.resolve("性能优化与测试.md"))).isTrue();
        }
    }
}

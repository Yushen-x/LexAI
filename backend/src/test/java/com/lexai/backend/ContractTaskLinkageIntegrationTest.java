package com.lexai.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 合同 ↔ 待办任务 双向联动：
 * <ul>
 *   <li>合同决策 APPROVED → 对应活跃待办置 COMPLETED；</li>
 *   <li>合同决策 NEEDS_REVISION → 对应活跃待办置 REJECTED；</li>
 *   <li>合同状态推进到 SIGNED 等阶段 → 对应活跃待办置 COMPLETED。</li>
 * </ul>
 *
 * <p>每个用例都先通过 /legal/contract-review 自行创建活跃待办，避免与其它测试类共享/争抢种子任务。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractTaskLinkageIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private TestRestTemplate client() {
        return new TestRestTemplate(restTemplateBuilder.rootUri("http://localhost:" + port + "/api"));
    }

    @Test
    void approveReview_completesActiveTask() {
        long contractId = 6L; // LX-2026-006，种子状态 COMPLETED，无活跃待办
        seedActiveReviewTask(contractId, "年度法律咨询服务框架协议");

        String body = """
                {
                  "reviewerOpinion":"风险已澄清，可进入签署。",
                  "reviewDecision":"APPROVED"
                }
                """;
        ResponseEntity<String> updateRes = client().exchange(
                "/contracts/" + contractId + "/review",
                HttpMethod.PUT,
                jsonEntity(body),
                String.class
        );
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertNoActiveTaskForRelatedId(contractId);
    }

    @Test
    void needsRevisionReview_rejectsActiveTask() {
        long contractId = 7L; // LX-2026-007，TERMINATED，无活跃待办
        seedActiveReviewTask(contractId, "仓储场地租赁合同（已终止示例）");

        String body = """
                {
                  "reviewerOpinion":"违约责任不清晰，需要业务团队补充。",
                  "reviewDecision":"NEEDS_REVISION"
                }
                """;
        ResponseEntity<String> updateRes = client().exchange(
                "/contracts/" + contractId + "/review",
                HttpMethod.PUT,
                jsonEntity(body),
                String.class
        );
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertNoActiveTaskForRelatedId(contractId);
    }

    @Test
    void contractStatusAdvance_completesActiveTask() {
        // LX-2026-005 种子状态 IN_PROGRESS，合法转换 IN_PROGRESS → COMPLETED
        long contractId = 5L;
        seedActiveReviewTask(contractId, "核心机房第二季度软硬件代维合同");

        String body = "{\"status\":\"COMPLETED\"}";
        ResponseEntity<String> updateRes = client().exchange(
                "/contracts/" + contractId + "/status",
                HttpMethod.PUT,
                jsonEntity(body),
                String.class
        );
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertNoActiveTaskForRelatedId(contractId);
    }

    private void seedActiveReviewTask(long contractId, String contractName) {
        String body = String.format("""
                {
                  "contractTitle":"%s",
                  "contractContent":"测试用合同正文，需补充关键条款",
                  "contractId":%d
                }
                """, contractName, contractId);
        ResponseEntity<String> seedRes =
                client().postForEntity("/legal/contract-review", jsonEntity(body), String.class);
        assertThat(seedRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        String tasks = client().getForEntity("/tasks", String.class).getBody();
        assertThat(tasks).contains("\"relatedId\":\"" + contractId + "\"");
    }

    private void assertNoActiveTaskForRelatedId(long contractId) {
        String pending = client().getForEntity("/tasks?status=PENDING", String.class).getBody();
        assertThat(pending).doesNotContain("\"relatedId\":\"" + contractId + "\"");
        String inProgress = client().getForEntity("/tasks?status=IN_PROGRESS", String.class).getBody();
        assertThat(inProgress).doesNotContain("\"relatedId\":\"" + contractId + "\"");
    }

    private static HttpEntity<String> jsonEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}

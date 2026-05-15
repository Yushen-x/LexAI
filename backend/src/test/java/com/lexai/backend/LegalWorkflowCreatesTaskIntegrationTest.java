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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * 待办任务联动行为：
 * <ul>
 *   <li>法律咨询、案件分析、合同起草等 AI 工具不再生成待办；</li>
 *   <li>合同审查仅在带 contractId 时生成 / 复用待办，并同合同 id 关联；</li>
 *   <li>同一合同的活跃审查待办做去重，不会重复刷出新任务。</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LegalWorkflowCreatesTaskIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private TestRestTemplate client() {
        return new TestRestTemplate(restTemplateBuilder.rootUri("http://localhost:" + port + "/api"));
    }

    private static int countTasks(String responseBody) {
        return responseBody == null ? 0 : responseBody.split("\"taskNo\"").length - 1;
    }

    @Test
    void consultation_doesNotCreateFollowUpTask() {
        ResponseEntity<String> before = client().getForEntity("/tasks", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"question\":\"劳动仲裁流程咨询\",\"facts\":[]}";
        HttpEntity<String> postEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> legalRes =
                client().postForEntity("/legal/consultation", postEntity, String.class);
        assertThat(legalRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(legalRes.getBody()).contains("SUCCESS");

        ResponseEntity<String> after = client().getForEntity("/tasks", String.class);
        assertThat(countTasks(after.getBody())).isEqualTo(countTasks(before.getBody()));
    }

    @Test
    void caseAnalysis_doesNotCreateFollowUpTask() {
        ResponseEntity<String> before = client().getForEntity("/tasks", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"caseSummary\":\"二手房买卖纠纷\",\"evidencePoints\":[]}";
        HttpEntity<String> postEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> legalRes =
                client().postForEntity("/legal/case-analysis", postEntity, String.class);
        assertThat(legalRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> after = client().getForEntity("/tasks", String.class);
        assertThat(countTasks(after.getBody())).isEqualTo(countTasks(before.getBody()));
    }

    @Test
    void contractReview_withoutContractId_doesNotCreateTask() {
        ResponseEntity<String> before = client().getForEntity("/tasks", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                  "contractTitle":"匿名试用合同",
                  "contractContent":"合同缺少保密条款"
                }
                """;
        HttpEntity<String> postEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> reviewRes =
                client().postForEntity("/legal/contract-review", postEntity, String.class);
        assertThat(reviewRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> after = client().getForEntity("/tasks", String.class);
        assertThat(countTasks(after.getBody())).isEqualTo(countTasks(before.getBody()));
    }

    @Test
    void contractReview_withContractId_createsOrReusesTaskKeyedByContractId() {
        // 合同 5 在种子里没有活跃审查待办，第一次审查应创建一条新待办，第二次审查应复用同一条。
        ResponseEntity<String> before = client().getForEntity("/tasks", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                  "contractTitle":"机房代维合同复审",
                  "contractContent":"合同缺少 SLA 量化指标和违约赔偿基数",
                  "contractId":5
                }
                """;
        HttpEntity<String> postEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> firstRes =
                client().postForEntity("/legal/contract-review", postEntity, String.class);
        assertThat(firstRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> afterFirst = client().getForEntity("/tasks", String.class);
        assertThat(countTasks(afterFirst.getBody())).isEqualTo(countTasks(before.getBody()) + 1);
        assertThat(afterFirst.getBody()).contains("\"relatedId\":\"5\"");
        assertThat(afterFirst.getBody()).contains("LX-2026-005");

        // 同一合同再次审查应复用同一条任务（去重）。
        ResponseEntity<String> secondRes =
                client().postForEntity("/legal/contract-review", postEntity, String.class);
        assertThat(secondRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> afterSecond = client().getForEntity("/tasks", String.class);
        assertThat(countTasks(afterSecond.getBody())).isEqualTo(countTasks(afterFirst.getBody()));
    }

    @Test
    void contractReview_persistsLatestReviewOnContract() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                  "contractTitle":"已落库合同审查",
                  "contractContent":"合同缺少保密条款和争议解决条款",
                  "contractId":1,
                  "createFollowUpTask":false
                }
                """;
        HttpEntity<String> postEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> reviewRes =
                client().postForEntity("/legal/contract-review", postEntity, String.class);
        assertThat(reviewRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> contractRes = client().getForEntity("/contracts/1", String.class);
        assertThat(contractRes.getBody()).contains("latestReview");
        assertThat(contractRes.getBody()).contains("PENDING_CONFIRMATION");
        assertThat(contractRes.getBody()).contains("summary");
    }
}

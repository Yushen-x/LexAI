package com.lexai.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 校验合同台账 API（Commit 5）：真实端口 + context-path /api。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContractApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    private TestRestTemplate client() {
        return new TestRestTemplate(restTemplateBuilder.rootUri("http://localhost:" + port + "/api"));
    }

    @Test
    void listContracts_returnsSuccessAndSeedContractNo() {
        ResponseEntity<String> res = client().getForEntity("/contracts?page=0&size=20", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).contains("SUCCESS");
        assertThat(res.getBody()).contains("LX-2026-001");
        assertThat(res.getBody()).contains("totalElements");
    }

    @Test
    void getContract_byId_returnsSeedFirstRow() {
        ResponseEntity<String> res = client().getForEntity("/contracts/1", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("LX-2026-001");
    }

    @Test
    void exportContracts_returnsCsvAttachment() {
        ResponseEntity<String> res = client().getForEntity(
                "/contracts/export?status=DRAFT",
                String.class
        );

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("contracts.csv");
        assertThat(res.getHeaders().getContentType()).isNotNull();
        assertThat(res.getBody()).contains("合同编号,合同名称,合同类型");
        assertThat(res.getBody()).contains("LX-2026-003");
    }

    @Test
    void getContract_missing_returns404() {
        ResponseEntity<String> res = client().getForEntity("/contracts/999999", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).contains("FAILED");
    }

    @Test
    void updateReview_persistsManualOpinion() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                  "reviewerOpinion":"人工确认违约责任需补充业务背景",
                  "reviewDecision":"NEEDS_REVISION"
                }
                """;

        ResponseEntity<String> updateRes = client().exchange(
                "/contracts/1/review",
                org.springframework.http.HttpMethod.PUT,
                new HttpEntity<>(body, headers),
                String.class
        );

        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateRes.getBody()).contains("人工确认违约责任需补充业务背景");
        assertThat(updateRes.getBody()).contains("NEEDS_REVISION");
    }

    @Test
    void contractReview_createsReviewHistoryRecord() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {
                  "contractTitle":"软件定制开发合同复审",
                  "contractContent":"合同未明确验收标准、知识产权归属和争议解决条款。",
                  "contractId":8,
                  "createFollowUpTask":false
                }
                """;

        ResponseEntity<String> reviewRes = client().postForEntity(
                "/legal/contract-review",
                new HttpEntity<>(body, headers),
                String.class
        );
        assertThat(reviewRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> listRes = client().getForEntity("/contracts/8/reviews", String.class);
        assertThat(listRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listRes.getBody()).contains("PENDING_CONFIRMATION");
        assertThat(listRes.getBody()).contains("riskCount");

        JsonNode first = objectMapper.readTree(listRes.getBody()).path("data").get(0);
        long reviewId = first.path("id").asLong();

        ResponseEntity<String> detailRes = client().getForEntity(
                "/contracts/8/reviews/" + reviewId,
                String.class
        );
        assertThat(detailRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailRes.getBody()).contains("missingClauses");
        assertThat(detailRes.getBody()).contains("risks");
    }
}

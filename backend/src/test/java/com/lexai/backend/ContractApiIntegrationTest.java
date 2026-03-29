package com.lexai.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
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
    void getContract_missing_returns404() {
        ResponseEntity<String> res = client().getForEntity("/contracts/999999", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).contains("FAILED");
    }
}

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
 * Commit 7：法律咨询等链路成功后应自动增加一条待办。
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

    @Test
    void consultation_createsFollowUpTask() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"question\":\"劳动仲裁流程咨询\",\"facts\":[]}";
        HttpEntity<String> postEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> legalRes =
                client().postForEntity("/legal/consultation", postEntity, String.class);
        assertThat(legalRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(legalRes.getBody()).contains("SUCCESS");

        ResponseEntity<String> tasksRes = client().getForEntity("/tasks", String.class);
        assertThat(tasksRes.getBody()).contains("法律咨询 - 待查阅结果");
        assertThat(tasksRes.getBody()).contains("WF-2026-007");
    }
}

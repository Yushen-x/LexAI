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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TaskApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    private TestRestTemplate client() {
        return new TestRestTemplate(restTemplateBuilder.rootUri("http://localhost:" + port + "/api"));
    }

    @Test
    void listTasks_returnsSeed() {
        ResponseEntity<String> res = client().getForEntity("/tasks", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("SUCCESS");
        assertThat(res.getBody()).contains("WF-2026-001");
    }

    @Test
    void listTasks_filterByStatus() {
        ResponseEntity<String> res = client().getForEntity("/tasks?status=PENDING", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("WF-2026-001");
    }

    @Test
    void getTask_byId() {
        ResponseEntity<String> res = client().getForEntity("/tasks/1", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("WF-2026-001");
    }

    @Test
    void getTask_missing_returns404() {
        ResponseEntity<String> res = client().getForEntity("/tasks/999999", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateTaskStatus() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // 用种子第 6 条（原 REJECTED），避免改坏 id=1 影响其它用例
        HttpEntity<String> entity = new HttpEntity<>("{\"status\":\"COMPLETED\"}", headers);
        ResponseEntity<String> res =
                client().exchange("/tasks/6/status", HttpMethod.PUT, entity, String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("COMPLETED");
    }
}

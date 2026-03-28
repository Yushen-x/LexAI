package com.lexai.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Commit 9：MockMvc 集成测试——合同列表、待办列表、法律咨询响应结构与前端字段对齐。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MockMvcWorkspaceApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getContracts_returnsPagedEnvelopeWithSeedData() throws Exception {
        mockMvc.perform(get("/contracts").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(8))
                .andExpect(jsonPath("$.data.totalElements").value(8))
                .andExpect(jsonPath("$.data.content[0].contractNo").value("LX-2026-001"));
    }

    @Test
    void getTasks_returnsListEnvelopeWithSeedData() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].taskNo").exists());
    }

    @Test
    void postConsultation_returnsFieldsExpectedByFrontend() throws Exception {
        String json = "{\"question\":\"MockMvc 集成测试咨询\",\"facts\":[]}";
        mockMvc.perform(post("/legal/consultation").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.category").exists())
                .andExpect(jsonPath("$.data.legalBasis").isArray())
                .andExpect(jsonPath("$.data.recommendations").isArray())
                .andExpect(jsonPath("$.data.riskAlerts").isArray());
    }
}

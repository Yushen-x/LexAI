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
                .andExpect(jsonPath("$.data.length()").value(4))
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

    @Test
    void getLegalSessions_returnsPagedHistoryAfterConsultation() throws Exception {
        String json = "{\"question\":\"历史持久化测试\",\"facts\":[]}";
        mockMvc.perform(post("/legal/consultation").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());

        mockMvc.perform(get("/legal/sessions").param("type", "CONSULTATION").param("page", "0").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].title").value("历史持久化测试"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getLegalSessions_supportsKeywordSearch() throws Exception {
        mockMvc.perform(post("/legal/consultation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"拖欠工资如何维权\",\"facts\":[]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/legal/consultation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"合同违约赔偿标准\",\"facts\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/legal/sessions")
                        .param("type", "CONSULTATION")
                        .param("keyword", "工资")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("拖欠工资如何维权"));
    }

    @Test
    void getRecentLegalSessions_returnsLatestAiActivity() throws Exception {
        mockMvc.perform(post("/legal/consultation").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Dashboard 最近活动测试\",\"facts\":[]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/legal/sessions/recent").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Dashboard 最近活动测试"));
    }

    @Test
    void getSystemHealth_returnsExtendedFields() throws Exception {
        mockMvc.perform(get("/system/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.aiMode").exists())
                .andExpect(jsonPath("$.data.database").exists())
                .andExpect(jsonPath("$.data.knowledgeDocumentCount").exists())
                .andExpect(jsonPath("$.data.consultationSessionCount").exists());
    }
}

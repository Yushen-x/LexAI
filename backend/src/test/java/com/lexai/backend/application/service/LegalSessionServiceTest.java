package com.lexai.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.LegalSessionListResponse;
import com.lexai.backend.application.dto.response.RetrievalContext;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.LegalScenarioType;
import com.lexai.backend.persistence.entity.LegalSessionEntity;
import com.lexai.backend.persistence.repository.LegalSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LegalSessionServiceTest {

    @Mock
    private LegalSessionRepository legalSessionRepository;

    private LegalSessionService legalSessionService;

    @BeforeEach
    void setUp() {
        legalSessionService = new LegalSessionService(legalSessionRepository, new ObjectMapper());
    }

    @Test
    void saveSession_persistsEntityWithGeneratedSessionNo() throws Exception {
        when(legalSessionRepository.findTopBySessionNoStartingWithOrderBySessionNoDesc(any()))
                .thenReturn(Optional.empty());

        ConsultationRequest input = new ConsultationRequest("测试问题", List.of("事实1"), false);
        ConsultationResponse output = new ConsultationResponse(
                "劳动",
                List.of("依据1"),
                List.of("建议1"),
                List.of("风险1"),
                0.9,
                new RetrievalContext(List.of(), List.of(), List.of()),
                "回答"
        );

        legalSessionService.saveSession(
                LegalScenarioType.CONSULTATION,
                "测试问题",
                input,
                output,
                0.9,
                "trace-1",
                "演示用户"
        );

        ArgumentCaptor<LegalSessionEntity> captor = ArgumentCaptor.forClass(LegalSessionEntity.class);
        verify(legalSessionRepository).save(captor.capture());
        LegalSessionEntity saved = captor.getValue();
        assertThat(saved.getSessionNo()).startsWith("LS-");
        assertThat(saved.getScenarioType()).isEqualTo(LegalScenarioType.CONSULTATION);
        assertThat(saved.getTitle()).isEqualTo("测试问题");
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
    }

    @Test
    void list_returnsPagedSummaries() {
        LegalSessionEntity entity = new LegalSessionEntity();
        entity.setId(1L);
        entity.setSessionNo("LS-2026-001");
        entity.setScenarioType(LegalScenarioType.CONSULTATION);
        entity.setTitle("历史咨询");
        entity.setConfidence(0.8);
        entity.setTraceId("trace");
        entity.setCreatedAt(Instant.parse("2026-03-01T10:00:00Z"));

        when(legalSessionRepository.findByScenarioTypeOrderByCreatedAtDesc(
                any(LegalScenarioType.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        LegalSessionListResponse response =
                legalSessionService.list(LegalScenarioType.CONSULTATION, null, 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).isEqualTo("历史咨询");
    }

    @Test
    void list_withKeyword_usesSearchRepository() {
        LegalSessionEntity entity = new LegalSessionEntity();
        entity.setId(2L);
        entity.setSessionNo("LS-2026-002");
        entity.setScenarioType(LegalScenarioType.CONSULTATION);
        entity.setTitle("劳动合同咨询");
        entity.setCreatedAt(Instant.parse("2026-03-02T10:00:00Z"));

        when(legalSessionRepository.searchByScenarioTypeAndKeyword(
                any(LegalScenarioType.class), any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        LegalSessionListResponse response =
                legalSessionService.list(LegalScenarioType.CONSULTATION, "劳动", 0, 10);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).title()).contains("劳动");
    }

    @Test
    void listRecent_returnsLatestSessions() {
        LegalSessionEntity entity = new LegalSessionEntity();
        entity.setId(3L);
        entity.setSessionNo("LS-2026-003");
        entity.setScenarioType(LegalScenarioType.CASE_ANALYSIS);
        entity.setTitle("案件分析");
        entity.setCreatedAt(Instant.parse("2026-03-03T10:00:00Z"));

        when(legalSessionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        assertThat(legalSessionService.listRecent(5)).hasSize(1);
    }

    @Test
    void getById_throwsWhenMissing() {
        when(legalSessionRepository.findById(99L)).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(
                ResourceNotFoundException.class,
                () -> legalSessionService.getById(99L)
        );
    }
}

package com.lexai.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.response.LegalSessionDetailResponse;
import com.lexai.backend.application.dto.response.LegalSessionListResponse;
import com.lexai.backend.application.dto.response.LegalSessionSummaryResponse;
import com.lexai.backend.application.service.support.SequenceGenerator;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.LegalScenarioType;
import com.lexai.backend.persistence.entity.LegalSessionEntity;
import com.lexai.backend.persistence.repository.LegalSessionRepository;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalSessionService {

    private static final Logger log = LoggerFactory.getLogger(LegalSessionService.class);
    private static final String SESSION_NO_CODE = "LS";
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Asia/Shanghai"));

    private final LegalSessionRepository legalSessionRepository;
    private final ObjectMapper objectMapper;
    private final Object sessionNoLock = new Object();

    public LegalSessionService(LegalSessionRepository legalSessionRepository, ObjectMapper objectMapper) {
        this.legalSessionRepository = legalSessionRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public LegalSessionListResponse list(LegalScenarioType scenarioType, String keyword, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 50);
        Page<LegalSessionEntity> result = querySessions(scenarioType, keyword, PageRequest.of(safePage, safeSize));
        List<LegalSessionSummaryResponse> content = result.getContent().stream()
                .map(this::toSummary)
                .toList();
        return new LegalSessionListResponse(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public List<LegalSessionSummaryResponse> listRecent(int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 20);
        return legalSessionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private Page<LegalSessionEntity> querySessions(
            LegalScenarioType scenarioType,
            String keyword,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.strip();
        if (normalizedKeyword.isEmpty()) {
            return legalSessionRepository.findByScenarioTypeOrderByCreatedAtDesc(scenarioType, pageable);
        }
        return legalSessionRepository.searchByScenarioTypeAndKeyword(scenarioType, normalizedKeyword, pageable);
    }

    @Transactional(readOnly = true)
    public LegalSessionDetailResponse getById(long id) {
        LegalSessionEntity entity = legalSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("历史会话不存在"));
        return toDetail(entity);
    }

    @Transactional
    public void saveSession(
            LegalScenarioType scenarioType,
            String title,
            Object input,
            Object output,
            Double confidence,
            String traceId,
            String initiator
    ) {
        try {
            LegalSessionEntity entity = new LegalSessionEntity();
            entity.setSessionNo(nextSessionNo());
            entity.setScenarioType(scenarioType);
            entity.setTitle(truncateTitle(title));
            entity.setInputPayload(objectMapper.writeValueAsString(input));
            entity.setOutputPayload(objectMapper.writeValueAsString(output));
            entity.setConfidence(confidence);
            entity.setTraceId(traceId);
            entity.setInitiator(initiator == null || initiator.isBlank() ? "演示用户" : initiator.strip());
            legalSessionRepository.save(entity);
            log.info("法律会话已持久化 sessionNo={} scenario={}", entity.getSessionNo(), scenarioType);
        } catch (JsonProcessingException exception) {
            log.warn("法律会话持久化失败（JSON 序列化）scenario={}: {}", scenarioType, exception.toString());
        } catch (Exception exception) {
            log.warn("法律会话持久化失败 scenario={}: {}", scenarioType, exception.toString());
        }
    }

    @Transactional(readOnly = true)
    public long countByScenario(LegalScenarioType scenarioType) {
        return legalSessionRepository.countByScenarioType(scenarioType);
    }

    private String nextSessionNo() {
        synchronized (sessionNoLock) {
            String prefix = SequenceGenerator.buildPrefixForCurrentYear(SESSION_NO_CODE);
            String current = legalSessionRepository.findTopBySessionNoStartingWithOrderBySessionNoDesc(prefix)
                    .map(LegalSessionEntity::getSessionNo)
                    .orElse(null);
            long next = SequenceGenerator.nextSequence(current, prefix);
            return SequenceGenerator.format(SESSION_NO_CODE, Year.now().getValue(), next);
        }
    }

    private static String truncateTitle(String title) {
        if (title == null || title.isBlank()) {
            return "未命名会话";
        }
        String trimmed = title.strip();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed;
    }

    private LegalSessionSummaryResponse toSummary(LegalSessionEntity entity) {
        return new LegalSessionSummaryResponse(
                entity.getId(),
                entity.getSessionNo(),
                entity.getScenarioType(),
                entity.getTitle(),
                entity.getConfidence(),
                entity.getTraceId(),
                formatInstant(entity.getCreatedAt())
        );
    }

    private LegalSessionDetailResponse toDetail(LegalSessionEntity entity) {
        return new LegalSessionDetailResponse(
                entity.getId(),
                entity.getSessionNo(),
                entity.getScenarioType(),
                entity.getTitle(),
                entity.getInputPayload(),
                entity.getOutputPayload(),
                entity.getConfidence(),
                entity.getTraceId(),
                entity.getInitiator(),
                formatInstant(entity.getCreatedAt())
        );
    }

    private static String formatInstant(java.time.Instant instant) {
        if (instant == null) {
            return "";
        }
        return ISO_FORMATTER.format(instant);
    }
}

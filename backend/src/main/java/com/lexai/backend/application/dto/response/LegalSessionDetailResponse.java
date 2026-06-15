package com.lexai.backend.application.dto.response;

import com.lexai.backend.domain.model.LegalScenarioType;

public record LegalSessionDetailResponse(
        long id,
        String sessionNo,
        LegalScenarioType scenarioType,
        String title,
        String inputPayload,
        String outputPayload,
        Double confidence,
        String traceId,
        String initiator,
        String createdAt
) {
}

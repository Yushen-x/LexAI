package com.lexai.backend.application.dto.response;

import com.lexai.backend.domain.model.LegalScenarioType;

public record LegalSessionSummaryResponse(
        long id,
        String sessionNo,
        LegalScenarioType scenarioType,
        String title,
        Double confidence,
        String traceId,
        String createdAt
) {
}

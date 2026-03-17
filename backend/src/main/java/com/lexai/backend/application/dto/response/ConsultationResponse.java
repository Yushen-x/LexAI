package com.lexai.backend.application.dto.response;

import java.util.List;

public record ConsultationResponse(
        String category,
        List<String> legalBasis,
        List<String> recommendations,
        List<String> riskAlerts
) {
}


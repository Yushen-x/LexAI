package com.lexai.backend.application.dto.response;

import com.lexai.backend.domain.model.RiskLevel;

public record ContractRiskItem(
        RiskLevel level,
        String clause,
        String issue,
        String suggestion
) {
}


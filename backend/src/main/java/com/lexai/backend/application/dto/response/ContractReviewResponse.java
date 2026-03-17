package com.lexai.backend.application.dto.response;

import java.util.List;

public record ContractReviewResponse(
        List<ContractRiskItem> risks,
        List<String> missingClauses,
        String summary
) {
}


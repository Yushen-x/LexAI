package com.lexai.backend.application.dto.contract;

import com.lexai.backend.application.dto.response.ContractRiskItem;
import java.time.Instant;
import java.util.List;

public record ContractLatestReview(
        String summary,
        List<ContractRiskItem> risks,
        List<String> missingClauses,
        String reviewerOpinion,
        String reviewDecision,
        Instant reviewedAt
) {
}

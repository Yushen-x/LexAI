package com.lexai.backend.application.dto.contract;

import java.time.Instant;

public record ContractReviewRecordSummary(
        Long id,
        Long contractId,
        String contractNo,
        String contractName,
        String contractType,
        String summary,
        int riskCount,
        int missingClauseCount,
        String reviewDecision,
        String reviewerOpinion,
        Double confidence,
        String source,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

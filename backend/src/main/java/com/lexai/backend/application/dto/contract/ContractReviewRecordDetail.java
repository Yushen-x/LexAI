package com.lexai.backend.application.dto.contract;

import com.lexai.backend.application.dto.response.ContractRiskItem;
import java.time.Instant;
import java.util.List;

public record ContractReviewRecordDetail(
        Long id,
        Long contractId,
        String contractNo,
        String contractName,
        String contractType,
        String summary,
        List<ContractRiskItem> risks,
        List<String> missingClauses,
        String reviewDecision,
        String reviewerOpinion,
        Double confidence,
        String source,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

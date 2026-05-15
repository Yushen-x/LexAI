package com.lexai.backend.application.dto.contract;

public record UpdateContractReviewRequest(
        String reviewerOpinion,
        String reviewDecision
) {
    public UpdateContractReviewRequest {
        reviewerOpinion = reviewerOpinion == null ? "" : reviewerOpinion.trim();
        reviewDecision = reviewDecision == null ? "" : reviewDecision.trim();
    }
}

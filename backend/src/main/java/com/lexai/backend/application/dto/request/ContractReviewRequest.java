package com.lexai.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ContractReviewRequest(
        @NotBlank(message = "contractTitle must not be blank")
        String contractTitle,
        @NotBlank(message = "contractContent must not be blank")
        String contractContent
) {
}


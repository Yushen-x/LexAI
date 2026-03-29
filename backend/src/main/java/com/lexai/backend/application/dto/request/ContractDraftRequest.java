package com.lexai.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ContractDraftRequest(
        @NotBlank(message = "contractName must not be blank")
        String contractName,

        @NotBlank(message = "contractType must not be blank")
        String contractType,

        @NotBlank(message = "partyA must not be blank")
        String partyA,

        @NotBlank(message = "partyB must not be blank")
        String partyB,

        @Positive(message = "amount must be positive")
        Long amount,

        String duration,

        String requirements
) {
}

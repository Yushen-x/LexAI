package com.lexai.backend.application.dto.contract;

import com.lexai.backend.domain.model.ContractStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record ContractResponse(
        Long id,
        String contractNo,
        String name,
        String contractType,
        String partyA,
        String partyB,
        BigDecimal amount,
        ContractStatus status,
        String source,
        Instant createdAt,
        Instant updatedAt
) {
}

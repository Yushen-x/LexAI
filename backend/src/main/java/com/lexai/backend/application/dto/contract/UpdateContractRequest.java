package com.lexai.backend.application.dto.contract;

import com.lexai.backend.domain.model.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record UpdateContractRequest(
        @NotBlank(message = "鍚堝悓鍚嶇О涓嶈兘涓虹┖") String name,
        @NotBlank(message = "鍚堝悓绫诲瀷涓嶈兘涓虹┖") String contractType,
        String partyA,
        String partyB,
        BigDecimal amount,
        String content,
        String source,
        ContractStatus status
) {
    public UpdateContractRequest {
        name = name == null ? null : name.trim();
        contractType = contractType == null ? null : contractType.trim();
        partyA = partyA == null ? "" : partyA.trim();
        partyB = partyB == null ? "" : partyB.trim();
        content = content == null ? "" : content.trim();
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (status == null) {
            status = ContractStatus.DRAFT;
        }
    }
}

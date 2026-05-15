package com.lexai.backend.application.dto.contract;

import com.lexai.backend.domain.model.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateContractRequest(
        @NotBlank(message = "合同名称不能为空") String name,
        @NotBlank(message = "合同类型不能为空") String contractType,
        String partyA,
        String partyB,
        BigDecimal amount,
        String content,
        String source,
        ContractStatus status
) {
    public CreateContractRequest {
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

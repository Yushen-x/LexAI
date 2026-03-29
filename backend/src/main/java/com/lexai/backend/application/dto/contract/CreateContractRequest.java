package com.lexai.backend.application.dto.contract;

import com.lexai.backend.domain.model.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CreateContractRequest(
        @NotBlank(message = "合同名称不能为空") String name,
        @NotBlank(message = "合同类型不能为空") String contractType,
        @NotBlank(message = "甲方不能为空") String partyA,
        @NotBlank(message = "乙方不能为空") String partyB,
        BigDecimal amount,
        String source,
        ContractStatus status
) {
    public CreateContractRequest {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (status == null) {
            status = ContractStatus.DRAFT;
        }
    }
}

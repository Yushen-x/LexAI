package com.lexai.backend.application.dto.contract;

import com.lexai.backend.domain.model.ContractStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateContractStatusRequest(
        @NotNull(message = "状态不能为空") ContractStatus status
) {
}

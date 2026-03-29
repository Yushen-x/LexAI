package com.lexai.backend.application.dto.contract;

import java.util.List;

public record ContractListResponse(
        List<ContractResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}

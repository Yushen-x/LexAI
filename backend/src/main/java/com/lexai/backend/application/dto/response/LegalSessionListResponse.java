package com.lexai.backend.application.dto.response;

import java.util.List;

public record LegalSessionListResponse(
        List<LegalSessionSummaryResponse> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}

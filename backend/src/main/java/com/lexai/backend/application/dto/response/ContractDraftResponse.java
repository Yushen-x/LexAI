package com.lexai.backend.application.dto.response;

import java.time.LocalDateTime;

public record ContractDraftResponse(
        String generatedContent,
        String summary,
        LocalDateTime generatedAt
) {
}

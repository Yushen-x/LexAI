package com.lexai.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CaseAnalysisRequest(
        @NotBlank(message = "caseSummary must not be blank")
        String caseSummary,
        List<String> evidencePoints
) {
}


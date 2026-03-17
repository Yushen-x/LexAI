package com.lexai.backend.application.dto.response;

import java.util.List;

public record CaseAnalysisResponse(
        List<String> keyFacts,
        List<String> disputedIssues,
        List<String> evidenceGaps,
        List<String> suggestedActions
) {
}


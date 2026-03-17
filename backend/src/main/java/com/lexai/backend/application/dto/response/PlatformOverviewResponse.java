package com.lexai.backend.application.dto.response;

import java.util.List;

public record PlatformOverviewResponse(
        String projectName,
        String positioning,
        List<String> technicalHighlights,
        List<String> capabilities
) {
}


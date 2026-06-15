package com.lexai.backend.application.dto.response;

public record SystemHealthResponse(
        String status,
        String aiMode,
        String database,
        int knowledgeDocumentCount,
        int knowledgeChunkCount,
        long consultationSessionCount,
        long caseAnalysisSessionCount
) {
}

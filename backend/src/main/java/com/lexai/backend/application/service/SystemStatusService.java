package com.lexai.backend.application.service;

import com.lexai.backend.application.dto.response.SystemHealthResponse;
import com.lexai.backend.domain.model.LegalScenarioType;
import com.lexai.backend.infrastructure.ai.LocalKnowledgeSearchClient;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemStatusService {

    private final LegalSessionService legalSessionService;
    private final LocalKnowledgeSearchClient localKnowledgeSearchClient;
    private final DataSource dataSource;
    private final String aiMode;

    public SystemStatusService(
            LegalSessionService legalSessionService,
            LocalKnowledgeSearchClient localKnowledgeSearchClient,
            DataSource dataSource,
            @Value("${lexai.ai.mode:mock}") String aiMode
    ) {
        this.legalSessionService = legalSessionService;
        this.localKnowledgeSearchClient = localKnowledgeSearchClient;
        this.dataSource = dataSource;
        this.aiMode = aiMode;
    }

    @Transactional(readOnly = true)
    public SystemHealthResponse health() {
        LocalKnowledgeSearchClient.KnowledgeIndexStats stats = localKnowledgeSearchClient.getIndexStats();
        return new SystemHealthResponse(
                "UP",
                aiMode,
                resolveDatabaseLabel(),
                stats.documentCount(),
                stats.chunkCount(),
                legalSessionService.countByScenario(LegalScenarioType.CONSULTATION),
                legalSessionService.countByScenario(LegalScenarioType.CASE_ANALYSIS)
        );
    }

    private String resolveDatabaseLabel() {
        try (var connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null) {
                return "Unknown";
            }
            if (product.toLowerCase().contains("h2")) {
                return "H2 内存库";
            }
            if (product.toLowerCase().contains("mysql")) {
                return "MySQL";
            }
            return product;
        } catch (Exception ignored) {
            return "Unknown";
        }
    }
}

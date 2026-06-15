package com.lexai.backend.persistence.entity;

import com.lexai.backend.domain.model.LegalScenarioType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "legal_sessions")
public class LegalSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_no", nullable = false, unique = true, length = 64)
    private String sessionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false, length = 32)
    private LegalScenarioType scenarioType;

    @Column(nullable = false, length = 512)
    private String title;

    @Lob
    @Column(name = "input_payload", nullable = false, columnDefinition = "LONGTEXT")
    private String inputPayload;

    @Lob
    @Column(name = "output_payload", nullable = false, columnDefinition = "LONGTEXT")
    private String outputPayload;

    @Column
    private Double confidence;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(nullable = false, length = 256)
    private String initiator;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public LegalScenarioType getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(LegalScenarioType scenarioType) {
        this.scenarioType = scenarioType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public void setInputPayload(String inputPayload) {
        this.inputPayload = inputPayload;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(String outputPayload) {
        this.outputPayload = outputPayload;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

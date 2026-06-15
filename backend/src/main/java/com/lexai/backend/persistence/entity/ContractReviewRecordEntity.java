package com.lexai.backend.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "contract_review_records", indexes = {
        @Index(name = "idx_review_record_contract_created", columnList = "contract_id, created_at"),
        @Index(name = "idx_review_record_contract_reviewed", columnList = "contract_id, reviewed_at"),
        @Index(name = "idx_review_record_decision", columnList = "review_decision")
})
public class ContractReviewRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "contract_no", nullable = false, length = 64)
    private String contractNo;

    @Column(name = "contract_name", nullable = false, length = 512)
    private String contractName;

    @Column(name = "contract_type", nullable = false, length = 128)
    private String contractType;

    @Lob
    @Column(name = "review_summary", columnDefinition = "LONGTEXT")
    private String reviewSummary;

    @Lob
    @Column(name = "review_risks_json", columnDefinition = "LONGTEXT")
    private String reviewRisksJson;

    @Lob
    @Column(name = "review_missing_clauses_json", columnDefinition = "LONGTEXT")
    private String reviewMissingClausesJson;

    @Lob
    @Column(name = "reviewer_opinion", columnDefinition = "LONGTEXT")
    private String reviewerOpinion;

    @Column(name = "review_decision", nullable = false, length = 64)
    private String reviewDecision;

    @Column(precision = 8)
    private Double confidence;

    @Column(length = 128)
    private String source;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (reviewedAt == null) {
            reviewedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getContractName() {
        return contractName;
    }

    public void setContractName(String contractName) {
        this.contractName = contractName;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getReviewSummary() {
        return reviewSummary;
    }

    public void setReviewSummary(String reviewSummary) {
        this.reviewSummary = reviewSummary;
    }

    public String getReviewRisksJson() {
        return reviewRisksJson;
    }

    public void setReviewRisksJson(String reviewRisksJson) {
        this.reviewRisksJson = reviewRisksJson;
    }

    public String getReviewMissingClausesJson() {
        return reviewMissingClausesJson;
    }

    public void setReviewMissingClausesJson(String reviewMissingClausesJson) {
        this.reviewMissingClausesJson = reviewMissingClausesJson;
    }

    public String getReviewerOpinion() {
        return reviewerOpinion;
    }

    public void setReviewerOpinion(String reviewerOpinion) {
        this.reviewerOpinion = reviewerOpinion;
    }

    public String getReviewDecision() {
        return reviewDecision;
    }

    public void setReviewDecision(String reviewDecision) {
        this.reviewDecision = reviewDecision;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

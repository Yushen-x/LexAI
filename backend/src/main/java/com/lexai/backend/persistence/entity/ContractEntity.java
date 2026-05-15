package com.lexai.backend.persistence.entity;

import com.lexai.backend.domain.model.ContractStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "contracts")
public class ContractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_no", nullable = false, unique = true, length = 64)
    private String contractNo;

    @Column(nullable = false, length = 512)
    private String name;

    @Column(name = "contract_type", nullable = false, length = 128)
    private String contractType;

    @Column(name = "party_a", nullable = false, length = 256)
    private String partyA;

    @Column(name = "party_b", nullable = false, length = 256)
    private String partyB;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ContractStatus status;

    @Column(length = 256)
    private String source;

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

    @Column(name = "review_decision", length = 64)
    private String reviewDecision;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
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

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public String getPartyA() {
        return partyA;
    }

    public void setPartyA(String partyA) {
        this.partyA = partyA;
    }

    public String getPartyB() {
        return partyB;
    }

    public void setPartyB(String partyB) {
        this.partyB = partyB;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

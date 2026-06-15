package com.lexai.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.contract.ContractReviewRecordDetail;
import com.lexai.backend.application.dto.contract.ContractReviewRecordSummary;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.ContractRiskItem;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.persistence.entity.ContractEntity;
import com.lexai.backend.persistence.entity.ContractReviewRecordEntity;
import com.lexai.backend.persistence.repository.ContractRepository;
import com.lexai.backend.persistence.repository.ContractReviewRecordRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractReviewRecordService {

    private static final String DEFAULT_REVIEW_DECISION = "PENDING_CONFIRMATION";
    private static final String DEFAULT_SOURCE = "AI_REVIEW";

    private final ContractReviewRecordRepository reviewRecordRepository;
    private final ContractRepository contractRepository;
    private final ObjectMapper objectMapper;

    public ContractReviewRecordService(
            ContractReviewRecordRepository reviewRecordRepository,
            ContractRepository contractRepository,
            ObjectMapper objectMapper
    ) {
        this.reviewRecordRepository = reviewRecordRepository;
        this.contractRepository = contractRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ContractReviewRecordDetail recordAiReview(ContractEntity contract, ContractReviewResponse review) {
        ContractReviewRecordEntity entity = new ContractReviewRecordEntity();
        entity.setContractId(contract.getId());
        entity.setContractNo(defaultText(contract.getContractNo()));
        entity.setContractName(defaultText(contract.getName()));
        entity.setContractType(defaultText(contract.getContractType()));
        entity.setReviewSummary(abbreviate(defaultText(review.summary()), 2000));
        entity.setReviewRisksJson(writeJson(compactRisks(review.risks())));
        entity.setReviewMissingClausesJson(writeJson(compactStrings(review.missingClauses(), 12, 120)));
        entity.setReviewerOpinion("");
        entity.setReviewDecision(DEFAULT_REVIEW_DECISION);
        entity.setConfidence(review.confidence());
        entity.setSource(DEFAULT_SOURCE);
        entity.setReviewedAt(Instant.now());
        return toDetail(reviewRecordRepository.save(entity));
    }

    @Transactional
    public void syncLatestManualReview(long contractId, String reviewDecision, String reviewerOpinion) {
        reviewRecordRepository.findTopByContractIdOrderByReviewedAtDescIdDesc(contractId)
                .ifPresent(entity -> {
                    entity.setReviewDecision(normalizeReviewDecision(reviewDecision));
                    entity.setReviewerOpinion(defaultText(reviewerOpinion));
                    reviewRecordRepository.save(entity);
                });
    }

    @Transactional(readOnly = true)
    public List<ContractReviewRecordSummary> listByContract(long contractId) {
        ensureActiveContract(contractId);
        return reviewRecordRepository.findByContractIdOrderByReviewedAtDescIdDesc(contractId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ContractReviewRecordDetail getByContract(long contractId, long reviewId) {
        ensureActiveContract(contractId);
        ContractReviewRecordEntity entity = reviewRecordRepository.findByIdAndContractId(reviewId, contractId)
                .orElseThrow(() -> new ResourceNotFoundException("合同审查历史记录不存在"));
        return toDetail(entity);
    }

    private void ensureActiveContract(long contractId) {
        contractRepository.findById(contractId)
                .filter(contract -> !contract.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
    }

    private ContractReviewRecordSummary toSummary(ContractReviewRecordEntity entity) {
        List<ContractRiskItem> risks = readRisks(entity.getReviewRisksJson());
        List<String> missingClauses = readStringList(entity.getReviewMissingClausesJson());
        return new ContractReviewRecordSummary(
                entity.getId(),
                entity.getContractId(),
                entity.getContractNo(),
                entity.getContractName(),
                entity.getContractType(),
                defaultText(entity.getReviewSummary()),
                risks.size(),
                missingClauses.size(),
                normalizeReviewDecision(entity.getReviewDecision()),
                defaultText(entity.getReviewerOpinion()),
                entity.getConfidence(),
                defaultText(entity.getSource()),
                entity.getReviewedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ContractReviewRecordDetail toDetail(ContractReviewRecordEntity entity) {
        return new ContractReviewRecordDetail(
                entity.getId(),
                entity.getContractId(),
                entity.getContractNo(),
                entity.getContractName(),
                entity.getContractType(),
                defaultText(entity.getReviewSummary()),
                readRisks(entity.getReviewRisksJson()),
                readStringList(entity.getReviewMissingClausesJson()),
                normalizeReviewDecision(entity.getReviewDecision()),
                defaultText(entity.getReviewerOpinion()),
                entity.getConfidence(),
                defaultText(entity.getSource()),
                entity.getReviewedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("合同审查历史记录序列化失败", exception);
        }
    }

    private List<ContractRiskItem> readRisks(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ContractRiskItem>>() {
            });
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException exception) {
            return Collections.emptyList();
        }
    }

    private static List<ContractRiskItem> compactRisks(List<ContractRiskItem> risks) {
        if (risks == null || risks.isEmpty()) {
            return List.of();
        }
        return risks.stream()
                .limit(8)
                .map(item -> new ContractRiskItem(
                        item.level(),
                        abbreviate(item.clause(), 120),
                        abbreviate(item.issue(), 300),
                        abbreviate(item.suggestion(), 300)
                ))
                .toList();
    }

    private static List<String> compactStrings(List<String> values, int limit, int maxLength) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .limit(limit)
                .map(value -> abbreviate(value, maxLength))
                .toList();
    }

    private static String normalizeReviewDecision(String reviewDecision) {
        if (!StringUtils.hasText(reviewDecision)) {
            return DEFAULT_REVIEW_DECISION;
        }
        return reviewDecision.trim().toUpperCase();
    }

    private static String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String abbreviate(String value, int maxLength) {
        String normalized = defaultText(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "…";
    }
}

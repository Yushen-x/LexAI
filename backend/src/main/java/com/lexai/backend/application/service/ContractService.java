package com.lexai.backend.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.contract.ContractLatestReview;
import com.lexai.backend.application.dto.contract.ContractListResponse;
import com.lexai.backend.application.dto.contract.ContractResponse;
import com.lexai.backend.application.dto.contract.CreateContractRequest;
import com.lexai.backend.application.dto.contract.UpdateContractReviewRequest;
import com.lexai.backend.application.dto.contract.UpdateContractRequest;
import com.lexai.backend.application.dto.contract.UpdateContractStatusRequest;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.ContractRiskItem;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.ContractStatus;
import com.lexai.backend.persistence.entity.ContractEntity;
import com.lexai.backend.persistence.repository.ContractRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractService {

    private static final String DEFAULT_SOURCE = "WORKSPACE_IMPORT";
    private static final String AI_DRAFT_SOURCE = "AI_DRAFT";
    private static final String DEFAULT_REVIEW_DECISION = "PENDING_CONFIRMATION";

    private final ContractRepository contractRepository;
    private final ObjectMapper objectMapper;
    private final TaskService taskService;
    private final Object contractNoLock = new Object();

    public ContractService(
            ContractRepository contractRepository,
            ObjectMapper objectMapper,
            TaskService taskService
    ) {
        this.contractRepository = contractRepository;
        this.objectMapper = objectMapper;
        this.taskService = taskService;
    }

    @Transactional(readOnly = true)
    public ContractListResponse list(
            String keyword,
            ContractStatus status,
            String contractType,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);

        Specification<ContractEntity> spec = Specification.where(notDeleted());
        if (StringUtils.hasText(keyword)) {
            spec = spec.and(keywordContains(keyword.trim()));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (StringUtils.hasText(contractType)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("contractType"), contractType.trim()));
        }

        Page<ContractEntity> result = contractRepository.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        List<ContractResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new ContractListResponse(
                content,
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public ContractResponse getById(long id) {
        ContractEntity entity = contractRepository.findById(id)
                .filter(contract -> !contract.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        return toResponse(entity);
    }

    @Transactional
    public ContractResponse create(CreateContractRequest request) {
        synchronized (contractNoLock) {
            String source = normalizeSource(request.source());
            String partyA = defaultText(request.partyA());
            String partyB = defaultText(request.partyB());

            if (AI_DRAFT_SOURCE.equals(source) && request.status() == ContractStatus.DRAFT) {
                Optional<ContractEntity> existingDraft = contractRepository
                        .findTopByDeletedFalseAndSourceAndStatusAndNameAndContractTypeAndPartyAAndPartyBOrderByUpdatedAtDesc(
                                source,
                                ContractStatus.DRAFT,
                                request.name(),
                                request.contractType(),
                                partyA,
                                partyB
                        );
                if (existingDraft.isPresent()) {
                    ContractEntity entity = existingDraft.get();
                    entity.setAmount(Optional.ofNullable(request.amount()).orElse(BigDecimal.ZERO));
                    entity.setContent(defaultText(request.content()));
                    entity.setSource(source);
                    entity.setStatus(request.status());
                    return toResponse(contractRepository.save(entity));
                }
            }

            ContractEntity entity = new ContractEntity();
            entity.setContractNo(nextContractNo());
            entity.setName(request.name());
            entity.setContractType(request.contractType());
            entity.setPartyA(partyA);
            entity.setPartyB(partyB);
            entity.setAmount(Optional.ofNullable(request.amount()).orElse(BigDecimal.ZERO));
            entity.setContent(defaultText(request.content()));
            entity.setStatus(request.status());
            entity.setSource(source);
            entity.setDeleted(false);
            ContractEntity saved = contractRepository.save(entity);
            return toResponse(saved);
        }
    }

    @Transactional
    public ContractResponse updateStatus(long id, UpdateContractStatusRequest request) {
        ContractEntity entity = contractRepository.findById(id)
                .filter(contract -> !contract.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));

        ContractStatus current = entity.getStatus();
        ContractStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new IllegalArgumentException(
                    String.format("合同状态不允许从「%s」变更为「%s」", current, target));
        }

        if (current == ContractStatus.UNDER_REVIEW && target == ContractStatus.SIGNED) {
            String decision = entity.getReviewDecision();
            if (!"APPROVED".equalsIgnoreCase(decision)) {
                throw new IllegalArgumentException("合同审查尚未通过，不能标记为已签署");
            }
        }

        entity.setStatus(target);
        ContractEntity saved = contractRepository.save(entity);
        taskService.closeContractReviewTaskOnContractStatus(saved.getId(), saved.getStatus().name());
        return toResponse(saved);
    }

    @Transactional
    public ContractResponse update(long id, UpdateContractRequest request) {
        ContractEntity entity = contractRepository.findById(id)
                .filter(contract -> !contract.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        entity.setName(request.name());
        entity.setContractType(request.contractType());
        entity.setPartyA(defaultText(request.partyA()));
        entity.setPartyB(defaultText(request.partyB()));
        entity.setAmount(Optional.ofNullable(request.amount()).orElse(BigDecimal.ZERO));
        entity.setContent(defaultText(request.content()));
        entity.setStatus(request.status());
        entity.setSource(normalizeSource(request.source(), entity.getSource()));
        return toResponse(contractRepository.save(entity));
    }

    @Transactional
    public ContractResponse saveAiReview(long id, ContractReviewResponse review) {
        ContractEntity entity = contractRepository.findById(id)
                .filter(contract -> !contract.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        List<ContractRiskItem> compactRisks = compactRisks(review.risks());
        List<String> compactMissingClauses = compactStrings(review.missingClauses(), 12, 120);
        entity.setReviewSummary(abbreviate(defaultText(review.summary()), 2000));
        entity.setReviewRisksJson(writeJson(compactRisks));
        entity.setReviewMissingClausesJson(writeJson(compactMissingClauses));
        entity.setReviewedAt(Instant.now());
        // 一旦触发新一轮 AI 审查，旧的人工决策即失效，强制回到「待人工确认」让审查闭环重新流转。
        entity.setReviewDecision(DEFAULT_REVIEW_DECISION);
        entity.setReviewerOpinion("");
        if (entity.getStatus() == ContractStatus.DRAFT) {
            entity.setStatus(ContractStatus.UNDER_REVIEW);
        }
        return toResponse(contractRepository.save(entity));
    }

    @Transactional
    public ContractResponse updateReview(long id, UpdateContractReviewRequest request) {
        ContractEntity entity = contractRepository.findById(id)
                .filter(contract -> !contract.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        String decision = normalizeReviewDecision(request.reviewDecision());
        entity.setReviewerOpinion(defaultText(request.reviewerOpinion()));
        entity.setReviewDecision(decision);
        if (entity.getReviewedAt() == null) {
            entity.setReviewedAt(Instant.now());
        }
        ContractEntity saved = contractRepository.save(entity);
        taskService.resolveContractReviewTask(saved.getId(), decision);
        return toResponse(saved);
    }

    @Transactional
    public void softDelete(long id) {
        ContractEntity entity = contractRepository.findById(id)
                .filter(contract -> !contract.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        entity.setDeleted(true);
        contractRepository.save(entity);
    }

    private String nextContractNo() {
        int year = Year.now().getValue();
        String prefix = "LX-" + year + "-";
        long sequence = contractRepository.findTopByContractNoStartingWithOrderByContractNoDesc(prefix)
                .map(ContractEntity::getContractNo)
                .map(contractNo -> nextSequence(contractNo, prefix))
                .orElse(1L);
        return String.format("LX-%d-%03d", year, sequence);
    }

    private static long nextSequence(String currentNo, String prefix) {
        if (currentNo == null || !currentNo.startsWith(prefix)) {
            return 1L;
        }
        String suffix = currentNo.substring(prefix.length());
        try {
            return Long.parseLong(suffix) + 1;
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }

    private static String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeSource(String source) {
        return normalizeSource(source, DEFAULT_SOURCE);
    }

    private static String normalizeSource(String source, String fallback) {
        return StringUtils.hasText(source) ? source.trim() : fallback;
    }

    private static String normalizeReviewDecision(String reviewDecision) {
        if (!StringUtils.hasText(reviewDecision)) {
            return DEFAULT_REVIEW_DECISION;
        }
        return reviewDecision.trim().toUpperCase();
    }

    private static Specification<ContractEntity> notDeleted() {
        return (root, q, cb) -> cb.isFalse(root.get("deleted"));
    }

    private static Specification<ContractEntity> keywordContains(String keyword) {
        return (root, q, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
            Predicate noLike = cb.like(cb.lower(root.get("contractNo")), pattern);
            return cb.or(nameLike, noLike);
        };
    }

    private ContractResponse toResponse(ContractEntity entity) {
        return new ContractResponse(
                entity.getId(),
                entity.getContractNo(),
                entity.getName(),
                entity.getContractType(),
                entity.getPartyA(),
                entity.getPartyB(),
                entity.getAmount(),
                defaultText(entity.getContent()),
                entity.getStatus(),
                entity.getSource(),
                toLatestReview(entity),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ContractLatestReview toLatestReview(ContractEntity entity) {
        boolean hasReview = StringUtils.hasText(entity.getReviewSummary())
                || StringUtils.hasText(entity.getReviewRisksJson())
                || StringUtils.hasText(entity.getReviewMissingClausesJson())
                || StringUtils.hasText(entity.getReviewerOpinion())
                || StringUtils.hasText(entity.getReviewDecision())
                || entity.getReviewedAt() != null;
        if (!hasReview) {
            return null;
        }

        return new ContractLatestReview(
                defaultText(entity.getReviewSummary()),
                readRisks(entity.getReviewRisksJson()),
                readStringList(entity.getReviewMissingClausesJson()),
                defaultText(entity.getReviewerOpinion()),
                normalizeReviewDecision(entity.getReviewDecision()),
                entity.getReviewedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("合同审查结果序列化失败", exception);
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

    private static String abbreviate(String value, int maxLength) {
        String normalized = defaultText(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "…";
    }
}

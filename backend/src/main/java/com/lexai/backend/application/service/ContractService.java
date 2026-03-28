package com.lexai.backend.application.service;

import com.lexai.backend.application.dto.contract.ContractListResponse;
import com.lexai.backend.application.dto.contract.ContractResponse;
import com.lexai.backend.application.dto.contract.CreateContractRequest;
import com.lexai.backend.application.dto.contract.UpdateContractStatusRequest;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.ContractStatus;
import com.lexai.backend.persistence.entity.ContractEntity;
import com.lexai.backend.persistence.repository.ContractRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Year;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContractService {

    private final ContractRepository contractRepository;

    public ContractService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
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
        ContractEntity e = contractRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        return toResponse(e);
    }

    @Transactional
    public ContractResponse create(CreateContractRequest request) {
        ContractEntity e = new ContractEntity();
        e.setContractNo(nextContractNo());
        e.setName(request.name());
        e.setContractType(request.contractType());
        e.setPartyA(request.partyA());
        e.setPartyB(request.partyB());
        e.setAmount(request.amount());
        e.setStatus(request.status());
        e.setSource(StringUtils.hasText(request.source()) ? request.source() : "工作台录入");
        e.setDeleted(false);
        ContractEntity saved = contractRepository.save(e);
        return toResponse(saved);
    }

    @Transactional
    public ContractResponse updateStatus(long id, UpdateContractStatusRequest request) {
        ContractEntity e = contractRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        e.setStatus(request.status());
        return toResponse(contractRepository.save(e));
    }

    @Transactional
    public void softDelete(long id) {
        ContractEntity e = contractRepository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("合同不存在或已删除"));
        e.setDeleted(true);
        contractRepository.save(e);
    }

    private String nextContractNo() {
        int year = Year.now().getValue();
        long seq = contractRepository.countByDeletedFalse() + 1;
        return String.format("LX-%d-%03d", year, seq);
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

    private ContractResponse toResponse(ContractEntity e) {
        return new ContractResponse(
                e.getId(),
                e.getContractNo(),
                e.getName(),
                e.getContractType(),
                e.getPartyA(),
                e.getPartyB(),
                e.getAmount(),
                e.getStatus(),
                e.getSource(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

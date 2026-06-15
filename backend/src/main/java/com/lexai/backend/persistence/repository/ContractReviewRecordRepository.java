package com.lexai.backend.persistence.repository;

import com.lexai.backend.persistence.entity.ContractReviewRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractReviewRecordRepository extends JpaRepository<ContractReviewRecordEntity, Long> {

    List<ContractReviewRecordEntity> findByContractIdOrderByReviewedAtDescIdDesc(Long contractId);

    Optional<ContractReviewRecordEntity> findByIdAndContractId(Long id, Long contractId);

    Optional<ContractReviewRecordEntity> findTopByContractIdOrderByReviewedAtDescIdDesc(Long contractId);
}

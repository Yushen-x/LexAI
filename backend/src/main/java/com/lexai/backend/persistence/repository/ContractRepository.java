package com.lexai.backend.persistence.repository;

import com.lexai.backend.persistence.entity.ContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContractRepository extends JpaRepository<ContractEntity, Long>, JpaSpecificationExecutor<ContractEntity> {

    long countByDeletedFalse();
}

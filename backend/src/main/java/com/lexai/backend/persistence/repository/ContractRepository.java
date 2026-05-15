package com.lexai.backend.persistence.repository;

import com.lexai.backend.persistence.entity.ContractEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContractRepository extends JpaRepository<ContractEntity, Long>, JpaSpecificationExecutor<ContractEntity> {

    Optional<ContractEntity> findTopByContractNoStartingWithOrderByContractNoDesc(String prefix);

    Optional<ContractEntity> findTopByDeletedFalseAndSourceAndStatusAndNameAndContractTypeAndPartyAAndPartyBOrderByUpdatedAtDesc(
            String source,
            com.lexai.backend.domain.model.ContractStatus status,
            String name,
            String contractType,
            String partyA,
            String partyB
    );
}

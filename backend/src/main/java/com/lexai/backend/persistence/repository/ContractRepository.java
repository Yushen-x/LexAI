package com.lexai.backend.persistence.repository;

import com.lexai.backend.persistence.entity.ContractEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    long countByDeletedFalse();

    long countByDeletedFalseAndStatus(com.lexai.backend.domain.model.ContractStatus status);

    @Query("SELECT c.status AS status, COUNT(c) AS cnt " +
           "FROM ContractEntity c WHERE c.deleted = false GROUP BY c.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT c.contractType AS contractType, COUNT(c) AS cnt " +
           "FROM ContractEntity c WHERE c.deleted = false GROUP BY c.contractType")
    List<Object[]> countGroupByType();

    @Query("SELECT FUNCTION('YEAR', c.createdAt) AS yr, " +
           "FUNCTION('MONTH', c.createdAt) AS mo, COUNT(c) AS cnt " +
           "FROM ContractEntity c " +
           "WHERE c.deleted = false AND c.createdAt >= :since " +
           "GROUP BY FUNCTION('YEAR', c.createdAt), FUNCTION('MONTH', c.createdAt) " +
           "ORDER BY yr ASC, mo ASC")
    List<Object[]> monthlyCreation(@Param("since") Instant since);
}

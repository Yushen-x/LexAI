package com.lexai.backend.persistence.repository;

import com.lexai.backend.domain.model.LegalScenarioType;
import com.lexai.backend.persistence.entity.LegalSessionEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LegalSessionRepository extends JpaRepository<LegalSessionEntity, Long> {

    Page<LegalSessionEntity> findByScenarioTypeOrderByCreatedAtDesc(
            LegalScenarioType scenarioType,
            Pageable pageable
    );

    @Query("""
            SELECT s FROM LegalSessionEntity s
            WHERE s.scenarioType = :type
              AND (LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.sessionNo) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY s.createdAt DESC
            """)
    Page<LegalSessionEntity> searchByScenarioTypeAndKeyword(
            @Param("type") LegalScenarioType type,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Page<LegalSessionEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<LegalSessionEntity> findTopBySessionNoStartingWithOrderBySessionNoDesc(String prefix);

    long countByScenarioType(LegalScenarioType scenarioType);
}

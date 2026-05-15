package com.lexai.backend.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 合同台账状态（与 TASK.md 合同模块一致）。
 *
 * 合法状态转换：
 *   DRAFT         → UNDER_REVIEW | TERMINATED
 *   UNDER_REVIEW  → SIGNED | DRAFT | TERMINATED
 *   SIGNED        → IN_PROGRESS | TERMINATED
 *   IN_PROGRESS   → COMPLETED | TERMINATED
 *   COMPLETED     → (终态)
 *   TERMINATED    → (终态)
 */
public enum ContractStatus {
    DRAFT,
    UNDER_REVIEW,
    SIGNED,
    IN_PROGRESS,
    COMPLETED,
    TERMINATED;

    private static final Map<ContractStatus, Set<ContractStatus>> ALLOWED = Map.of(
            DRAFT,         EnumSet.of(UNDER_REVIEW, TERMINATED),
            UNDER_REVIEW,  EnumSet.of(SIGNED, DRAFT, TERMINATED),
            SIGNED,        EnumSet.of(IN_PROGRESS, TERMINATED),
            IN_PROGRESS,   EnumSet.of(COMPLETED, TERMINATED),
            COMPLETED,     EnumSet.noneOf(ContractStatus.class),
            TERMINATED,    EnumSet.noneOf(ContractStatus.class)
    );

    public boolean canTransitionTo(ContractStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}

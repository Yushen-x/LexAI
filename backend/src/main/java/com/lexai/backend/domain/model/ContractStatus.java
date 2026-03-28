package com.lexai.backend.domain.model;

/**
 * 合同台账状态（与 TASK.md 合同模块一致）。
 */
public enum ContractStatus {
    DRAFT,
    UNDER_REVIEW,
    SIGNED,
    IN_PROGRESS,
    COMPLETED,
    TERMINATED
}

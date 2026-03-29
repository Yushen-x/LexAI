package com.lexai.backend.application.dto.task;

import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import com.lexai.backend.domain.model.WorkspaceTaskType;
import java.time.Instant;

public record TaskResponse(
        Long id,
        String taskNo,
        String title,
        WorkspaceTaskType type,
        String relatedId,
        String initiator,
        WorkspaceTaskStatus status,
        Instant createdAt
) {
}

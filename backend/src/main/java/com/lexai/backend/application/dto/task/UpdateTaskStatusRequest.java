package com.lexai.backend.application.dto.task;

import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
        @NotNull(message = "状态不能为空") WorkspaceTaskStatus status
) {
}

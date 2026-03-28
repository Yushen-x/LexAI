package com.lexai.backend.interfaces.rest;

import com.lexai.backend.application.dto.task.TaskResponse;
import com.lexai.backend.application.dto.task.UpdateTaskStatusRequest;
import com.lexai.backend.application.service.TaskService;
import com.lexai.backend.common.api.ApiResponse;
import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    /** 列表：可选按状态筛选；默认按创建时间倒序。 */
    @GetMapping
    public ApiResponse<List<TaskResponse>> list(@RequestParam(required = false) WorkspaceTaskStatus status) {
        return ApiResponse.success(taskService.list(status));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskResponse> get(@PathVariable long id) {
        return ApiResponse.success(taskService.getById(id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<TaskResponse> updateStatus(
            @PathVariable long id,
            @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return ApiResponse.success(taskService.updateStatus(id, request));
    }
}

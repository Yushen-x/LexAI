package com.lexai.backend.application.service;

import com.lexai.backend.application.dto.task.TaskResponse;
import com.lexai.backend.application.dto.task.UpdateTaskStatusRequest;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import com.lexai.backend.domain.model.WorkspaceTaskType;
import com.lexai.backend.persistence.entity.TaskEntity;
import com.lexai.backend.persistence.repository.TaskRepository;
import java.time.Year;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> list(WorkspaceTaskStatus status) {
        List<TaskEntity> rows =
                status == null
                        ? taskRepository.findAllByOrderByCreatedAtDesc()
                        : taskRepository.findByStatusOrderByCreatedAtDesc(status);
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getById(long id) {
        TaskEntity e = taskRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("待办任务不存在"));
        return toResponse(e);
    }

    @Transactional
    public TaskResponse updateStatus(long id, UpdateTaskStatusRequest request) {
        TaskEntity e = taskRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("待办任务不存在"));
        e.setStatus(request.status());
        return toResponse(taskRepository.save(e));
    }

    /**
     * 法律工作台各链路成功后调用：自动生成流水号与标题，状态为待处理。
     */
    @Transactional
    public TaskResponse createAfterLegalWorkflow(WorkspaceTaskType type, String relatedId, String initiator) {
        String title =
                switch (type) {
                    case LEGAL_CONSULTATION -> "法律咨询 - 待查阅结果";
                    case CASE_ANALYSIS -> "案件分析 - 待查阅结果";
                    case CONTRACT_REVIEW -> "合同审查 - 待确认";
                    case CONTRACT_DRAFT -> "合同起草 - 待确认";
                };
        return create(nextTaskNo(), title, type, relatedId, initiator, WorkspaceTaskStatus.PENDING);
    }

    /** 供法律工作台等业务在完成后写入待办（Commit 7 使用）。 */
    @Transactional
    public TaskResponse create(
            String taskNo,
            String title,
            WorkspaceTaskType type,
            String relatedId,
            String initiator,
            WorkspaceTaskStatus status
    ) {
        TaskEntity e = new TaskEntity();
        e.setTaskNo(taskNo);
        e.setTitle(title);
        e.setType(type);
        e.setRelatedId(relatedId);
        e.setInitiator(initiator);
        e.setStatus(status);
        return toResponse(taskRepository.save(e));
    }

    private String nextTaskNo() {
        int year = Year.now().getValue();
        long seq = taskRepository.count() + 1;
        return String.format("WF-%d-%03d", year, seq);
    }

    private TaskResponse toResponse(TaskEntity e) {
        return new TaskResponse(
                e.getId(),
                e.getTaskNo(),
                e.getTitle(),
                e.getType(),
                e.getRelatedId(),
                e.getInitiator(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }
}

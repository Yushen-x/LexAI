package com.lexai.backend.application.service;

import com.lexai.backend.application.dto.task.TaskResponse;
import com.lexai.backend.application.dto.task.UpdateTaskStatusRequest;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import com.lexai.backend.domain.model.WorkspaceTaskType;
import com.lexai.backend.persistence.entity.TaskEntity;
import com.lexai.backend.persistence.repository.TaskRepository;
import java.time.Year;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 待办任务（合同生命周期闭环）服务。
 *
 * <p>设计取舍：</p>
 * <ul>
 *   <li>仅围绕「合同审查」生成可执行待办——法律咨询、案件分析、合同起草属于查询/工具操作，不再产生待办。</li>
 *   <li>同一合同的活跃审查待办做去重，避免重复审查反复刷出新任务。</li>
 *   <li>合同状态/审查决策变化时联动关闭或驳回对应待办，形成闭环。</li>
 * </ul>
 */
@Service
public class TaskService {

    private static final EnumSet<WorkspaceTaskStatus> ACTIVE_STATUSES =
            EnumSet.of(WorkspaceTaskStatus.PENDING, WorkspaceTaskStatus.IN_PROGRESS);

    private final TaskRepository taskRepository;
    private final Object taskNoLock = new Object();

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
        TaskEntity entity = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("待办任务不存在"));
        return toResponse(entity);
    }

    @Transactional
    public TaskResponse updateStatus(long id, UpdateTaskStatusRequest request) {
        TaskEntity entity = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("待办任务不存在"));
        entity.setStatus(request.status());
        return toResponse(taskRepository.save(entity));
    }

    /**
     * 为合同审查创建或复用一条「待人工确认」待办。同一合同已有未结束的审查任务时，
     * 仅刷新标题并保留原 PENDING/IN_PROGRESS 状态，避免重复推送。
     */
    @Transactional
    public TaskResponse createOrReuseContractReviewTask(
            long contractId,
            String contractNo,
            String contractName,
            String initiator
    ) {
        String relatedId = String.valueOf(contractId);
        String title = buildContractReviewTitle("合同审查待人工确认", contractNo, contractName);

        Optional<TaskEntity> active = findActiveContractReviewTask(relatedId);
        if (active.isPresent()) {
            TaskEntity entity = active.get();
            entity.setTitle(title);
            return toResponse(taskRepository.save(entity));
        }

        synchronized (taskNoLock) {
            return create(
                    nextTaskNo(),
                    title,
                    WorkspaceTaskType.CONTRACT_REVIEW,
                    relatedId,
                    initiator,
                    WorkspaceTaskStatus.PENDING
            );
        }
    }

    /**
     * 当合同审查决策落定时，关闭对应的活跃待办。
     *
     * <ul>
     *   <li>APPROVED → COMPLETED</li>
     *   <li>NEEDS_REVISION → REJECTED</li>
     *   <li>其他（如 PENDING_CONFIRMATION）→ 不动</li>
     * </ul>
     */
    @Transactional
    public void resolveContractReviewTask(long contractId, String reviewDecision) {
        if (reviewDecision == null) {
            return;
        }
        WorkspaceTaskStatus target = switch (reviewDecision.trim().toUpperCase()) {
            case "APPROVED" -> WorkspaceTaskStatus.COMPLETED;
            case "NEEDS_REVISION" -> WorkspaceTaskStatus.REJECTED;
            default -> null;
        };
        if (target == null) {
            return;
        }
        applyToActiveContractReviewTasks(contractId, target);
    }

    /**
     * 当合同状态推进到签署/执行/完成/终止等「审查阶段已结束」的状态时，
     * 把对应活跃待办置为 COMPLETED；其他状态不动。
     */
    @Transactional
    public void closeContractReviewTaskOnContractStatus(long contractId, String contractStatus) {
        if (contractStatus == null) {
            return;
        }
        switch (contractStatus) {
            case "SIGNED", "IN_PROGRESS", "COMPLETED", "TERMINATED" ->
                    applyToActiveContractReviewTasks(contractId, WorkspaceTaskStatus.COMPLETED);
            default -> {
                // DRAFT / UNDER_REVIEW 仍是审查阶段，不关闭。
            }
        }
    }

    @Transactional
    public TaskResponse create(
            String taskNo,
            String title,
            WorkspaceTaskType type,
            String relatedId,
            String initiator,
            WorkspaceTaskStatus status
    ) {
        TaskEntity entity = new TaskEntity();
        entity.setTaskNo(taskNo);
        entity.setTitle(title);
        entity.setType(type);
        entity.setRelatedId(relatedId);
        entity.setInitiator(initiator);
        entity.setStatus(status);
        return toResponse(taskRepository.save(entity));
    }

    private void applyToActiveContractReviewTasks(long contractId, WorkspaceTaskStatus target) {
        String relatedId = String.valueOf(contractId);
        List<TaskEntity> active = taskRepository
                .findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
                        WorkspaceTaskType.CONTRACT_REVIEW, relatedId, ACTIVE_STATUSES);
        if (active.isEmpty()) {
            return;
        }
        for (TaskEntity entity : active) {
            entity.setStatus(target);
        }
        taskRepository.saveAll(active);
    }

    private Optional<TaskEntity> findActiveContractReviewTask(String relatedId) {
        return taskRepository
                .findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
                        WorkspaceTaskType.CONTRACT_REVIEW, relatedId, ACTIVE_STATUSES)
                .stream()
                .findFirst();
    }

    private String buildContractReviewTitle(String prefix, String contractNo, String contractName) {
        StringBuilder sb = new StringBuilder(prefix);
        if (contractNo != null && !contractNo.isBlank()) {
            sb.append(" · ").append(contractNo.trim());
        }
        if (contractName != null && !contractName.isBlank()) {
            sb.append(" · ").append(contractName.trim());
        }
        String result = sb.toString();
        return result.length() > 480 ? result.substring(0, 480) + "…" : result;
    }

    private String nextTaskNo() {
        int year = Year.now().getValue();
        String prefix = "WF-" + year + "-";
        long sequence = taskRepository.findTopByTaskNoStartingWithOrderByTaskNoDesc(prefix)
                .map(TaskEntity::getTaskNo)
                .map(taskNo -> nextSequence(taskNo, prefix))
                .orElse(1L);
        return String.format("WF-%d-%03d", year, sequence);
    }

    private static long nextSequence(String currentNo, String prefix) {
        if (currentNo == null || !currentNo.startsWith(prefix)) {
            return 1L;
        }
        String suffix = currentNo.substring(prefix.length());
        try {
            return Long.parseLong(suffix) + 1;
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }

    private TaskResponse toResponse(TaskEntity entity) {
        return new TaskResponse(
                entity.getId(),
                entity.getTaskNo(),
                entity.getTitle(),
                entity.getType(),
                entity.getRelatedId(),
                entity.getInitiator(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}

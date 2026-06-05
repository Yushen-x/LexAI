package com.lexai.backend.application.service;

import com.lexai.backend.application.dto.task.TaskResponse;
import com.lexai.backend.application.dto.task.UpdateTaskStatusRequest;
import com.lexai.backend.application.service.support.SequenceGenerator;
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
 *   <li>同一合同每次发起审查都保留一条流程记录，新的申请会覆盖旧的活跃记录。</li>
 *   <li>合同状态/审查决策变化时联动关闭或驳回最新活跃记录，形成闭环。</li>
 * </ul>
 */
@Service
public class TaskService {

    private static final EnumSet<WorkspaceTaskStatus> ACTIVE_STATUSES =
            EnumSet.of(WorkspaceTaskStatus.PENDING, WorkspaceTaskStatus.IN_PROGRESS);
    private static final String TASK_NO_CODE = "WF";

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
     * 为合同审查创建一条「待人工确认」流程记录。同一合同已有未结束记录时，
     * 先将旧记录标记为已覆盖，再保留本次申请的新记录。
     */
    @Transactional
    public TaskResponse createContractReviewTask(
            long contractId,
            String contractNo,
            String contractName,
            String initiator
    ) {
        String relatedId = String.valueOf(contractId);
        String title = buildContractReviewTitle("合同审查待人工确认", contractNo, contractName);

        markActiveContractReviewTasksSuperseded(relatedId);

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
        applyToLatestActiveContractReviewTask(contractId, target);
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
                    applyToLatestActiveContractReviewTask(contractId, WorkspaceTaskStatus.COMPLETED);
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

    private void markActiveContractReviewTasksSuperseded(String relatedId) {
        List<TaskEntity> active = taskRepository
                .findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
                        WorkspaceTaskType.CONTRACT_REVIEW, relatedId, ACTIVE_STATUSES);
        if (active.isEmpty()) {
            return;
        }
        for (TaskEntity entity : active) {
            entity.setStatus(WorkspaceTaskStatus.SUPERSEDED);
        }
        taskRepository.saveAll(active);
    }

    private void applyToLatestActiveContractReviewTask(long contractId, WorkspaceTaskStatus target) {
        String relatedId = String.valueOf(contractId);
        findActiveContractReviewTask(relatedId).ifPresent(entity -> {
            entity.setStatus(target);
            taskRepository.save(entity);
        });
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
        String prefix = SequenceGenerator.buildPrefix(TASK_NO_CODE, year);
        long sequence = taskRepository.findTopByTaskNoStartingWithOrderByTaskNoDesc(prefix)
                .map(TaskEntity::getTaskNo)
                .map(taskNo -> SequenceGenerator.nextSequence(taskNo, prefix))
                .orElse(1L);
        return SequenceGenerator.format(TASK_NO_CODE, year, sequence);
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

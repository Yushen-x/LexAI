package com.lexai.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lexai.backend.application.dto.task.TaskResponse;
import com.lexai.backend.application.dto.task.UpdateTaskStatusRequest;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import com.lexai.backend.domain.model.WorkspaceTaskType;
import com.lexai.backend.persistence.entity.TaskEntity;
import com.lexai.backend.persistence.repository.TaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link TaskService} 单元测试：用 Mockito 隔离仓储，聚焦审查记录生成、状态联动闭环等业务规则。
 */
class TaskServiceTest {

    private TaskRepository taskRepository;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = Mockito.mock(TaskRepository.class);
        taskService = new TaskService(taskRepository);
        // 默认：save 原样返回入参，便于断言落库内容
        when(taskRepository.save(any(TaskEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TaskEntity activeReviewTask(long id, WorkspaceTaskStatus status) {
        TaskEntity entity = new TaskEntity();
        entity.setId(id);
        entity.setTaskNo("WF-2026-00" + id);
        entity.setTitle("旧标题");
        entity.setType(WorkspaceTaskType.CONTRACT_REVIEW);
        entity.setRelatedId("100");
        entity.setInitiator("tester");
        entity.setStatus(status);
        return entity;
    }

    @Test
    @DisplayName("list(null) 走全量查询，list(status) 走按状态查询")
    void list_dispatchesByStatusPresence() {
        when(taskRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(taskRepository.findByStatusOrderByCreatedAtDesc(WorkspaceTaskStatus.PENDING))
                .thenReturn(List.of(activeReviewTask(1, WorkspaceTaskStatus.PENDING)));

        assertThat(taskService.list(null)).isEmpty();
        assertThat(taskService.list(WorkspaceTaskStatus.PENDING)).hasSize(1);

        verify(taskRepository).findAllByOrderByCreatedAtDesc();
        verify(taskRepository).findByStatusOrderByCreatedAtDesc(WorkspaceTaskStatus.PENDING);
    }

    @Test
    @DisplayName("getById 命中返回，未命中抛 ResourceNotFoundException")
    void getById_missingThrows() {
        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(activeReviewTask(1, WorkspaceTaskStatus.PENDING)));
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(taskService.getById(1L).id()).isEqualTo(1L);
        assertThatThrownBy(() -> taskService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateStatus 持久化新状态")
    void updateStatus_persistsNewStatus() {
        TaskEntity entity = activeReviewTask(1, WorkspaceTaskStatus.PENDING);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(entity));

        TaskResponse res = taskService.updateStatus(
                1L, new UpdateTaskStatusRequest(WorkspaceTaskStatus.IN_PROGRESS));

        assertThat(res.status()).isEqualTo(WorkspaceTaskStatus.IN_PROGRESS);
        assertThat(entity.getStatus()).isEqualTo(WorkspaceTaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("已有活跃审查记录时置为已覆盖，并为本次申请新建 PENDING 记录")
    void createContractReviewTask_supersedesActiveTaskAndCreatesNewOne() {
        TaskEntity active = activeReviewTask(7, WorkspaceTaskStatus.PENDING);
        when(taskRepository.findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
                eq(WorkspaceTaskType.CONTRACT_REVIEW), eq("100"), any()))
                .thenReturn(List.of(active));
        TaskEntity last = activeReviewTask(9, WorkspaceTaskStatus.COMPLETED);
        last.setTaskNo("WF-2026-009");
        when(taskRepository.findTopByTaskNoStartingWithOrderByTaskNoDesc(anyString()))
                .thenReturn(Optional.of(last));

        TaskResponse res = taskService.createContractReviewTask(
                100L, "LX-2026-001", "采购合同", "alice");

        assertThat(active.getStatus()).isEqualTo(WorkspaceTaskStatus.SUPERSEDED);
        assertThat(res.taskNo()).isEqualTo("WF-2026-010");
        assertThat(res.title()).contains("LX-2026-001").contains("采购合同");
        assertThat(res.status()).isEqualTo(WorkspaceTaskStatus.PENDING);
        verify(taskRepository).saveAll(List.of(active));
    }

    @Test
    @DisplayName("无活跃待办时新建 PENDING 任务并生成顺延任务号")
    void createContractReviewTask_createsNewWithNextNo() {
        when(taskRepository.findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
                eq(WorkspaceTaskType.CONTRACT_REVIEW), eq("100"), any()))
                .thenReturn(List.of());
        TaskEntity last = activeReviewTask(3, WorkspaceTaskStatus.COMPLETED);
        last.setTaskNo("WF-2026-003");
        when(taskRepository.findTopByTaskNoStartingWithOrderByTaskNoDesc(anyString()))
                .thenReturn(Optional.of(last));

        TaskResponse res = taskService.createContractReviewTask(
                100L, "LX-2026-001", "采购合同", "alice");

        assertThat(res.status()).isEqualTo(WorkspaceTaskStatus.PENDING);
        assertThat(res.taskNo()).isEqualTo("WF-2026-004");
        assertThat(res.type()).isEqualTo(WorkspaceTaskType.CONTRACT_REVIEW);
    }

    @Test
    @DisplayName("resolveContractReviewTask：只处理最新活跃记录，APPROVED→COMPLETED，NEEDS_REVISION→REJECTED")
    void resolve_mapsDecisionToTerminalStatusOnLatestActiveTask() {
        TaskEntity latest = activeReviewTask(2, WorkspaceTaskStatus.PENDING);
        TaskEntity older = activeReviewTask(1, WorkspaceTaskStatus.IN_PROGRESS);
        when(taskRepository.findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
                eq(WorkspaceTaskType.CONTRACT_REVIEW), eq("100"), any()))
                .thenReturn(List.of(latest, older));

        taskService.resolveContractReviewTask(100L, "approved");
        assertThat(latest.getStatus()).isEqualTo(WorkspaceTaskStatus.COMPLETED);
        assertThat(older.getStatus()).isEqualTo(WorkspaceTaskStatus.IN_PROGRESS);

        latest.setStatus(WorkspaceTaskStatus.PENDING);
        taskService.resolveContractReviewTask(100L, "NEEDS_REVISION");
        assertThat(latest.getStatus()).isEqualTo(WorkspaceTaskStatus.REJECTED);
        verify(taskRepository, Mockito.times(2)).save(latest);
    }

    @Test
    @DisplayName("resolveContractReviewTask：未知/空决策不触碰任何任务")
    void resolve_unknownDecisionIsNoop() {
        taskService.resolveContractReviewTask(100L, "PENDING_CONFIRMATION");
        taskService.resolveContractReviewTask(100L, null);

        verify(taskRepository, never())
                .findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(any(), any(), any());
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("合同推进到 SIGNED 关闭活跃待办，DRAFT/UNDER_REVIEW 不动")
    void closeOnContractStatus_onlyClosesWhenReviewPhaseEnded() {
        TaskEntity active = activeReviewTask(1, WorkspaceTaskStatus.PENDING);
        when(taskRepository.findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
                eq(WorkspaceTaskType.CONTRACT_REVIEW), eq("100"), any()))
                .thenReturn(List.of(active));

        taskService.closeContractReviewTaskOnContractStatus(100L, "SIGNED");
        assertThat(active.getStatus()).isEqualTo(WorkspaceTaskStatus.COMPLETED);

        taskService.closeContractReviewTaskOnContractStatus(100L, "DRAFT");
        // DRAFT 不触发额外保存
        verify(taskRepository, Mockito.times(1)).save(active);
    }
}

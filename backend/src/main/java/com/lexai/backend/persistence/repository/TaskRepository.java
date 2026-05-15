package com.lexai.backend.persistence.repository;

import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import com.lexai.backend.domain.model.WorkspaceTaskType;
import com.lexai.backend.persistence.entity.TaskEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByStatusOrderByCreatedAtDesc(WorkspaceTaskStatus status);

    List<TaskEntity> findAllByOrderByCreatedAtDesc();

    Optional<TaskEntity> findByTaskNo(String taskNo);

    Optional<TaskEntity> findTopByTaskNoStartingWithOrderByTaskNoDesc(String prefix);

    /**
     * 查找指定业务对象上「未结束」的任务（用于去重创建与状态联动）。
     */
    List<TaskEntity> findByTypeAndRelatedIdAndStatusInOrderByCreatedAtDesc(
            WorkspaceTaskType type,
            String relatedId,
            Collection<WorkspaceTaskStatus> statuses
    );
}

package com.lexai.backend.persistence.repository;

import com.lexai.backend.domain.model.WorkspaceTaskStatus;
import com.lexai.backend.persistence.entity.TaskEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByStatusOrderByCreatedAtDesc(WorkspaceTaskStatus status);

    List<TaskEntity> findAllByOrderByCreatedAtDesc();

    Optional<TaskEntity> findByTaskNo(String taskNo);
}

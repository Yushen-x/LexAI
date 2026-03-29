package com.lexai.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.lexai.backend.persistence.repository.ContractRepository;
import com.lexai.backend.persistence.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 校验 {@code data.sql} 在启动后是否成功灌入演示数据（Commit 4）。
 */
@SpringBootTest
class DataSqlSeedIntegrationTest {

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void seedDataLoaded() {
        assertThat(contractRepository.count()).isEqualTo(8);
        assertThat(taskRepository.count()).isEqualTo(6);
        assertThat(contractRepository.findById(1L)).isPresent();
        assertThat(taskRepository.findByTaskNo("WF-2026-001")).isPresent();
    }
}

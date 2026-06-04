package com.lexai.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lexai.backend.domain.model.ContractStatus;
import com.lexai.backend.persistence.repository.ContractRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link ContractStatisticsService} 单元测试：Mockito 隔离仓储，校验各统计口径的
 * 聚合行映射、空值兜底与 summary 组装。
 */
class ContractStatisticsServiceTest {

    private ContractRepository contractRepository;
    private ContractStatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        contractRepository = Mockito.mock(ContractRepository.class);
        statisticsService = new ContractStatisticsService(contractRepository);
    }

    @Test
    @DisplayName("statusDistribution 映射聚合行，null 状态兜底为 UNKNOWN")
    void statusDistribution_mapsRowsAndDefaultsNull() {
        when(contractRepository.countGroupByStatus()).thenReturn(List.of(
                new Object[]{ContractStatus.DRAFT, 3L},
                new Object[]{null, 1L}
        ));

        List<Map<String, Object>> result = statisticsService.statusDistribution();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("status", "DRAFT").containsEntry("count", 3L);
        assertThat(result.get(1)).containsEntry("status", "UNKNOWN").containsEntry("count", 1L);
    }

    @Test
    @DisplayName("typeDistribution 映射聚合行，null 类型兜底为「其他」")
    void typeDistribution_mapsRowsAndDefaultsNull() {
        when(contractRepository.countGroupByType()).thenReturn(List.of(
                new Object[]{"采购", 5L},
                new Object[]{null, 2L}
        ));

        List<Map<String, Object>> result = statisticsService.typeDistribution();

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).containsEntry("type", "采购").containsEntry("count", 5L);
        assertThat(result.get(1)).containsEntry("type", "其他").containsEntry("count", 2L);
    }

    @Test
    @DisplayName("monthlyTrend 映射 year/month/count")
    void monthlyTrend_mapsYearMonthCount() {
        when(contractRepository.monthlyCreation(Mockito.any(Instant.class))).thenReturn(List.of(
                new Object[]{2026, 5, 4L},
                new Object[]{2026, 6, 7L}
        ));

        List<Map<String, Object>> result = statisticsService.monthlyTrend();

        assertThat(result).hasSize(2);
        assertThat(result.get(0))
                .containsEntry("year", 2026).containsEntry("month", 5).containsEntry("count", 4L);
        assertThat(result.get(1)).containsEntry("month", 6).containsEntry("count", 7L);
    }

    @Test
    @DisplayName("summary 组装总数与三个分布口径")
    void summary_assemblesAllSections() {
        when(contractRepository.countByDeletedFalse()).thenReturn(12L);
        when(contractRepository.countGroupByStatus())
                .thenReturn(List.<Object[]>of(new Object[]{ContractStatus.SIGNED, 8L}));
        when(contractRepository.countGroupByType())
                .thenReturn(List.<Object[]>of(new Object[]{"服务", 8L}));
        when(contractRepository.monthlyCreation(Mockito.any(Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[]{2026, 6, 12L}));

        Map<String, Object> summary = statisticsService.summary();

        assertThat(summary).containsKeys("total", "statusDistribution", "typeDistribution", "monthlyTrend");
        assertThat(summary.get("total")).isEqualTo(12L);
        assertThat((List<?>) summary.get("statusDistribution")).hasSize(1);
        assertThat((List<?>) summary.get("typeDistribution")).hasSize(1);
        assertThat((List<?>) summary.get("monthlyTrend")).hasSize(1);
    }

    @Test
    @DisplayName("无数据时各口径返回空集合，summary 仍可组装")
    void emptyData_returnsEmptyCollections() {
        when(contractRepository.countByDeletedFalse()).thenReturn(0L);
        when(contractRepository.countGroupByStatus()).thenReturn(List.of());
        when(contractRepository.countGroupByType()).thenReturn(List.of());
        when(contractRepository.monthlyCreation(Mockito.any(Instant.class))).thenReturn(List.of());

        assertThat(statisticsService.statusDistribution()).isEmpty();
        assertThat(statisticsService.typeDistribution()).isEmpty();
        assertThat(statisticsService.monthlyTrend()).isEmpty();
        assertThat(statisticsService.summary().get("total")).isEqualTo(0L);
    }
}

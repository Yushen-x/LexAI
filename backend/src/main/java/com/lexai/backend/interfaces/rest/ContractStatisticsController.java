package com.lexai.backend.interfaces.rest;

import com.lexai.backend.application.service.ContractStatisticsService;
import com.lexai.backend.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/contracts/statistics")
public class ContractStatisticsController {

    private final ContractStatisticsService statisticsService;

    public ContractStatisticsController(ContractStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> summary() {
        return ApiResponse.success(statisticsService.summary());
    }

    @GetMapping("/status")
    public ApiResponse<?> statusDistribution() {
        return ApiResponse.success(statisticsService.statusDistribution());
    }

    @GetMapping("/type")
    public ApiResponse<?> typeDistribution() {
        return ApiResponse.success(statisticsService.typeDistribution());
    }

    @GetMapping("/trend")
    public ApiResponse<?> monthlyTrend() {
        return ApiResponse.success(statisticsService.monthlyTrend());
    }
}

package com.lexai.backend.interfaces.rest;

import com.lexai.backend.application.dto.response.PlatformOverviewResponse;
import com.lexai.backend.application.dto.response.SystemHealthResponse;
import com.lexai.backend.application.service.LegalWorkspaceService;
import com.lexai.backend.application.service.SystemStatusService;
import com.lexai.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final LegalWorkspaceService legalWorkspaceService;
    private final SystemStatusService systemStatusService;

    public SystemController(
            LegalWorkspaceService legalWorkspaceService,
            SystemStatusService systemStatusService
    ) {
        this.legalWorkspaceService = legalWorkspaceService;
        this.systemStatusService = systemStatusService;
    }

    @GetMapping("/health")
    public ApiResponse<SystemHealthResponse> health() {
        return ApiResponse.success(systemStatusService.health());
    }

    @GetMapping("/overview")
    public ApiResponse<PlatformOverviewResponse> overview() {
        return ApiResponse.success(legalWorkspaceService.getOverview());
    }
}


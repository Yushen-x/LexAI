package com.lexai.backend.interfaces.rest;

import com.lexai.backend.application.dto.response.PlatformOverviewResponse;
import com.lexai.backend.application.service.LegalWorkspaceService;
import com.lexai.backend.common.api.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system")
public class SystemController {

    private final LegalWorkspaceService legalWorkspaceService;

    public SystemController(LegalWorkspaceService legalWorkspaceService) {
        this.legalWorkspaceService = legalWorkspaceService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }

    @GetMapping("/overview")
    public ApiResponse<PlatformOverviewResponse> overview() {
        return ApiResponse.success(legalWorkspaceService.getOverview());
    }
}


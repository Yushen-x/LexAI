package com.lexai.backend.interfaces.rest;

import com.lexai.backend.application.dto.response.LegalSessionDetailResponse;
import com.lexai.backend.application.dto.response.LegalSessionListResponse;
import com.lexai.backend.application.dto.response.LegalSessionSummaryResponse;
import com.lexai.backend.application.service.LegalSessionService;
import com.lexai.backend.common.api.ApiResponse;
import com.lexai.backend.domain.model.LegalScenarioType;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/legal/sessions")
public class LegalSessionController {

    private final LegalSessionService legalSessionService;

    public LegalSessionController(LegalSessionService legalSessionService) {
        this.legalSessionService = legalSessionService;
    }

    @GetMapping
    public ApiResponse<LegalSessionListResponse> list(
            @RequestParam LegalScenarioType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(legalSessionService.list(type, keyword, page, size));
    }

    @GetMapping("/recent")
    public ApiResponse<List<LegalSessionSummaryResponse>> recent(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ApiResponse.success(legalSessionService.listRecent(limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<LegalSessionDetailResponse> get(@PathVariable long id) {
        return ApiResponse.success(legalSessionService.getById(id));
    }
}

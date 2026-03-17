package com.lexai.backend.interfaces.rest;

import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.service.LegalWorkspaceService;
import com.lexai.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/legal")
public class LegalWorkspaceController {

    private final LegalWorkspaceService legalWorkspaceService;

    public LegalWorkspaceController(LegalWorkspaceService legalWorkspaceService) {
        this.legalWorkspaceService = legalWorkspaceService;
    }

    @PostMapping("/consultation")
    public ApiResponse<ConsultationResponse> consultation(@Valid @RequestBody ConsultationRequest request) {
        return ApiResponse.success(legalWorkspaceService.handleConsultation(request));
    }

    @PostMapping("/case-analysis")
    public ApiResponse<CaseAnalysisResponse> caseAnalysis(@Valid @RequestBody CaseAnalysisRequest request) {
        return ApiResponse.success(legalWorkspaceService.handleCaseAnalysis(request));
    }

    @PostMapping("/contract-review")
    public ApiResponse<ContractReviewResponse> contractReview(@Valid @RequestBody ContractReviewRequest request) {
        return ApiResponse.success(legalWorkspaceService.handleContractReview(request));
    }
}


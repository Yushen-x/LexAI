package com.lexai.backend.application.service;

import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractDraftRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractDraftResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.PlatformOverviewResponse;

public interface LegalWorkspaceService {

    PlatformOverviewResponse getOverview();

    ConsultationResponse handleConsultation(ConsultationRequest request);

    CaseAnalysisResponse handleCaseAnalysis(CaseAnalysisRequest request);

    ContractReviewResponse handleContractReview(ContractReviewRequest request);

    ContractDraftResponse handleContractDraft(ContractDraftRequest request);
}


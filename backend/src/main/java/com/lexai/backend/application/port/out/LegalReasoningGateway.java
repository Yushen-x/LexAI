package com.lexai.backend.application.port.out;

import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;

public interface LegalReasoningGateway {

    ConsultationResponse consult(ConsultationRequest request);

    CaseAnalysisResponse analyzeCase(CaseAnalysisRequest request);

    ContractReviewResponse reviewContract(ContractReviewRequest request);
}


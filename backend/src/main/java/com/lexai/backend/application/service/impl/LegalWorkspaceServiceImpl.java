package com.lexai.backend.application.service.impl;

import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.PlatformOverviewResponse;
import com.lexai.backend.application.port.out.LegalReasoningGateway;
import com.lexai.backend.application.service.LegalWorkspaceService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LegalWorkspaceServiceImpl implements LegalWorkspaceService {

    private final LegalReasoningGateway legalReasoningGateway;

    public LegalWorkspaceServiceImpl(LegalReasoningGateway legalReasoningGateway) {
        this.legalReasoningGateway = legalReasoningGateway;
    }

    @Override
    public PlatformOverviewResponse getOverview() {
        return new PlatformOverviewResponse(
                "LexAI",
                "面向法律咨询、案件分析与合同审查的高可信智能法律工作台",
                List.of(
                        "法律专用 RAG 检索链路",
                        "多智能体协同推理",
                        "结构化法律输出",
                        "可信审查与人工复核预留"
                ),
                List.of(
                        "法律咨询",
                        "案件分析",
                        "合同审查",
                        "文书生成",
                        "证据材料整理"
                )
        );
    }

    @Override
    public ConsultationResponse handleConsultation(ConsultationRequest request) {
        return legalReasoningGateway.consult(request);
    }

    @Override
    public CaseAnalysisResponse handleCaseAnalysis(CaseAnalysisRequest request) {
        return legalReasoningGateway.analyzeCase(request);
    }

    @Override
    public ContractReviewResponse handleContractReview(ContractReviewRequest request) {
        return legalReasoningGateway.reviewContract(request);
    }
}


package com.lexai.backend.application.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.lexai.backend.application.dto.contract.ContractResponse;
import com.lexai.backend.application.dto.request.CaseAnalysisRequest;
import com.lexai.backend.application.dto.request.ConsultationRequest;
import com.lexai.backend.application.dto.request.ContractDraftRequest;
import com.lexai.backend.application.dto.request.ContractReviewRequest;
import com.lexai.backend.application.dto.response.CaseAnalysisResponse;
import com.lexai.backend.application.dto.response.ConsultationResponse;
import com.lexai.backend.application.dto.response.ContractDraftResponse;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.PlatformOverviewResponse;
import com.lexai.backend.application.port.out.LegalReasoningGateway;
import com.lexai.backend.application.service.ContractService;
import com.lexai.backend.application.service.LegalWorkspaceService;
import com.lexai.backend.application.service.TaskService;
import com.lexai.backend.common.exception.UserFacingException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LegalWorkspaceServiceImpl implements LegalWorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(LegalWorkspaceServiceImpl.class);
    private static final String DEFAULT_TASK_INITIATOR = "演示用户";

    private final LegalReasoningGateway legalReasoningGateway;
    private final TaskService taskService;
    private final ContractService contractService;

    public LegalWorkspaceServiceImpl(
            LegalReasoningGateway legalReasoningGateway,
            TaskService taskService,
            ContractService contractService
    ) {
        this.legalReasoningGateway = legalReasoningGateway;
        this.taskService = taskService;
        this.contractService = contractService;
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
        // 法律咨询是「即问即答」AI 工具，结果直接展示给用户，不再生成待办（避免污染合同审查待办流）。
        return invokeGateway(
                "LEGAL_CONSULTATION",
                summarize(request.question()),
                () -> legalReasoningGateway.consult(request));
    }

    @Override
    public CaseAnalysisResponse handleCaseAnalysis(CaseAnalysisRequest request) {
        // 案件分析同上，定位为分析工具，不入待办流。
        return invokeGateway(
                "CASE_ANALYSIS",
                summarize(request.caseSummary()),
                () -> legalReasoningGateway.analyzeCase(request));
    }

    @Override
    public ContractReviewResponse handleContractReview(ContractReviewRequest request) {
        String hint = Objects.toString(request.contractTitle(), "")
                + " "
                + Objects.toString(request.contractContent(), "");
        ContractReviewResponse response = invokeGateway(
                "CONTRACT_REVIEW",
                hint.strip(),
                () -> legalReasoningGateway.reviewContract(request));

        if (request.contractId() == null) {
            // 没有关联合同 = 试用模式：仅返回 AI 结果，不落库、不建待办。
            return response;
        }

        ContractResponse savedContract = null;
        try {
            savedContract = contractService.saveAiReview(request.contractId(), response);
        } catch (Exception exception) {
            log.warn("合同审查结果落库失败 contractId={}: {}", request.contractId(), exception.toString());
        }

        if (request.shouldCreateFollowUpTask()) {
            tryCreateOrReuseContractReviewTask(request.contractId(), savedContract);
        }
        return response;
    }

    @Override
    public ContractDraftResponse handleContractDraft(ContractDraftRequest request) {
        // 合同起草是 AI 写作工具，是否落库由前端通过 /contracts API 显式决定，这里不再插入待办。
        return invokeGateway(
                "CONTRACT_DRAFT",
                summarize(request.contractName()),
                () -> legalReasoningGateway.draftContract(request));
    }

    private <T> T invokeGateway(String scenario, String inputSummary, Supplier<T> call) {
        long startNanos = System.nanoTime();
        String safeSummary = inputSummary == null ? "" : inputSummary;
        if (safeSummary.length() > 100) {
            safeSummary = safeSummary.substring(0, 100) + "…";
        }
        log.info("AI调用开始 scenario={} inputSummary={}", scenario, safeSummary);
        try {
            T result = call.get();
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.info("AI调用成功 scenario={} tookMs={}", scenario, tookMs);
            return result;
        } catch (Exception exception) {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.error("AI调用失败 scenario={} tookMs={}", scenario, tookMs, exception);
            throw wrapGatewayFailure(exception);
        }
    }

    private static String summarize(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) + "…" : trimmed;
    }

    private static RuntimeException wrapGatewayFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof java.net.http.HttpTimeoutException) {
                return new UserFacingException(HttpStatus.GATEWAY_TIMEOUT, "智能分析服务响应超时，请稍后重试。");
            }
            if (current instanceof JsonProcessingException) {
                return new UserFacingException(HttpStatus.BAD_GATEWAY, "智能分析结果解析失败，请稍后重试。", current);
            }
            String className = current.getClass().getName();
            if (className.contains("Timeout")) {
                return new UserFacingException(HttpStatus.GATEWAY_TIMEOUT, "智能分析服务响应超时，请稍后重试。");
            }
            String message = Optional.ofNullable(current.getMessage()).orElse("").toLowerCase();
            if (message.contains("429") || message.contains("too many requests") || message.contains("rate limit")) {
                return new UserFacingException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试。");
            }
            current = current.getCause();
        }
        return new UserFacingException(HttpStatus.BAD_GATEWAY, "智能分析暂不可用，请稍后重试。", exception);
    }

    private void tryCreateOrReuseContractReviewTask(long contractId, ContractResponse contract) {
        try {
            String contractNo = contract != null ? contract.contractNo() : null;
            String contractName = contract != null ? contract.name() : null;
            taskService.createOrReuseContractReviewTask(contractId, contractNo, contractName, DEFAULT_TASK_INITIATOR);
        } catch (Exception exception) {
            log.warn("生成合同审查待办失败 contractId={}: {}", contractId, exception.toString());
        }
    }
}

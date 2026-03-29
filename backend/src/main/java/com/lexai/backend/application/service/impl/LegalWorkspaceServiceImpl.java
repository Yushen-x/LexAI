package com.lexai.backend.application.service.impl;

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
import com.lexai.backend.application.service.LegalWorkspaceService;
import com.lexai.backend.application.service.TaskService;
import com.lexai.backend.common.exception.UserFacingException;
import com.lexai.backend.domain.model.WorkspaceTaskType;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LegalWorkspaceServiceImpl implements LegalWorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(LegalWorkspaceServiceImpl.class);

    /** 暂无登录用户时用固定展示名；后续可接网关用户信息 */
    private static final String DEFAULT_TASK_INITIATOR = "演示用户";

    private final LegalReasoningGateway legalReasoningGateway;
    private final TaskService taskService;

    public LegalWorkspaceServiceImpl(LegalReasoningGateway legalReasoningGateway, TaskService taskService) {
        this.legalReasoningGateway = legalReasoningGateway;
        this.taskService = taskService;
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
        ConsultationResponse response = invokeGateway(
                "LEGAL_CONSULTATION",
                summarize(request.question()),
                () -> legalReasoningGateway.consult(request));
        tryCreateFollowUpTask(WorkspaceTaskType.LEGAL_CONSULTATION);
        return response;
    }

    @Override
    public CaseAnalysisResponse handleCaseAnalysis(CaseAnalysisRequest request) {
        CaseAnalysisResponse response = invokeGateway(
                "CASE_ANALYSIS",
                summarize(request.caseSummary()),
                () -> legalReasoningGateway.analyzeCase(request));
        tryCreateFollowUpTask(WorkspaceTaskType.CASE_ANALYSIS);
        return response;
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
        tryCreateFollowUpTask(WorkspaceTaskType.CONTRACT_REVIEW);
        return response;
    }

    @Override
    public ContractDraftResponse handleContractDraft(ContractDraftRequest request) {
        ContractDraftResponse response = invokeGateway(
                "CONTRACT_DRAFT",
                summarize(request.contractName()),
                () -> legalReasoningGateway.draftContract(request));
        tryCreateFollowUpTask(WorkspaceTaskType.CONTRACT_DRAFT);
        return response;
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
        } catch (Exception e) {
            long tookMs = (System.nanoTime() - startNanos) / 1_000_000L;
            log.error("AI调用失败 scenario={} tookMs={}", scenario, tookMs, e);
            throw wrapGatewayFailure(e);
        }
    }

    private static String summarize(String text) {
        if (text == null) {
            return "";
        }
        String t = text.strip();
        return t.length() > 100 ? t.substring(0, 100) + "…" : t;
    }

    private static RuntimeException wrapGatewayFailure(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            if (cur instanceof java.net.http.HttpTimeoutException) {
                return new UserFacingException(
                        HttpStatus.GATEWAY_TIMEOUT, "智能分析服务响应超时，请稍后重试。");
            }
            if (cur instanceof JsonProcessingException) {
                return new UserFacingException(
                        HttpStatus.BAD_GATEWAY, "智能分析结果解析失败，请稍后重试。", cur);
            }
            String cls = cur.getClass().getName();
            if (cls.contains("Timeout")) {
                return new UserFacingException(
                        HttpStatus.GATEWAY_TIMEOUT, "智能分析服务响应超时，请稍后重试。");
            }
            String m = Optional.ofNullable(cur.getMessage()).orElse("").toLowerCase();
            if (m.contains("429") || m.contains("too many requests") || m.contains("rate limit")) {
                return new UserFacingException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试。");
            }
            cur = cur.getCause();
        }
        return new UserFacingException(HttpStatus.BAD_GATEWAY, "智能分析暂不可用，请稍后重试。", e);
    }

    /**
     * 主流程已成功返回后再记待办；若写库失败只打日志，不向用户暴露 500。
     */
    private void tryCreateFollowUpTask(WorkspaceTaskType type) {
        String relatedId = "op-" + type.name().toLowerCase().replace('_', '-') + "-" + UUID.randomUUID();
        try {
            taskService.createAfterLegalWorkflow(type, relatedId, DEFAULT_TASK_INITIATOR);
        } catch (Exception e) {
            log.warn("自动生成待办失败 type={} relatedId={}: {}", type, relatedId, e.toString());
        }
    }
}


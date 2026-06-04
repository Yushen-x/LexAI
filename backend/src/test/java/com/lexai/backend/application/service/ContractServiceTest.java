package com.lexai.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.contract.ContractResponse;
import com.lexai.backend.application.dto.contract.CreateContractRequest;
import com.lexai.backend.application.dto.contract.UpdateContractReviewRequest;
import com.lexai.backend.application.dto.contract.UpdateContractStatusRequest;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.ContractRiskItem;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.ContractStatus;
import com.lexai.backend.domain.model.RiskLevel;
import com.lexai.backend.persistence.entity.ContractEntity;
import com.lexai.backend.persistence.repository.ContractRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * {@link ContractService} 单元测试：Mockito 隔离仓储与联动的 TaskService，
 * 使用真实 ObjectMapper 验证审查结果的 JSON 落库与回读，覆盖状态机校验等边界。
 */
class ContractServiceTest {

    private ContractRepository contractRepository;
    private TaskService taskService;
    private ContractService contractService;

    @BeforeEach
    void setUp() {
        contractRepository = Mockito.mock(ContractRepository.class);
        taskService = Mockito.mock(TaskService.class);
        contractService = new ContractService(contractRepository, new ObjectMapper(), taskService);
        when(contractRepository.save(any(ContractEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ContractEntity contract(long id, ContractStatus status) {
        ContractEntity entity = new ContractEntity();
        entity.setId(id);
        entity.setContractNo("LX-2026-001");
        entity.setName("采购合同");
        entity.setContractType("采购");
        entity.setPartyA("甲方");
        entity.setPartyB("乙方");
        entity.setAmount(BigDecimal.TEN);
        entity.setContent("正文");
        entity.setStatus(status);
        entity.setSource("WORKSPACE_IMPORT");
        entity.setDeleted(false);
        return entity;
    }

    @Test
    @DisplayName("getById：已删除或不存在均抛 ResourceNotFoundException")
    void getById_deletedOrMissingThrows() {
        ContractEntity deleted = contract(1, ContractStatus.DRAFT);
        deleted.setDeleted(true);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(deleted));
        when(contractRepository.findById(2L)).thenReturn(Optional.empty());
        when(contractRepository.findById(3L))
                .thenReturn(Optional.of(contract(3, ContractStatus.DRAFT)));

        assertThatThrownBy(() -> contractService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> contractService.getById(2L))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(contractService.getById(3L).id()).isEqualTo(3L);
    }

    @Test
    @DisplayName("create：从空台账起算生成 LX-<当年>-001")
    void create_generatesFirstContractNo() {
        when(contractRepository.findTopByContractNoStartingWithOrderByContractNoDesc(anyString()))
                .thenReturn(Optional.empty());

        CreateContractRequest request = new CreateContractRequest(
                "技术服务合同", "服务", "甲", "乙",
                new BigDecimal("1000.00"), "正文", null, ContractStatus.DRAFT);

        ContractResponse res = contractService.create(request);

        String expectedNo = "LX-" + Year.now().getValue() + "-001";
        assertThat(res.contractNo()).isEqualTo(expectedNo);
        assertThat(res.name()).isEqualTo("技术服务合同");
        assertThat(res.status()).isEqualTo(ContractStatus.DRAFT);
    }

    @Test
    @DisplayName("updateStatus：合法流转持久化并联动关闭审查待办")
    void updateStatus_validTransitionTriggersTaskClosing() {
        ContractEntity entity = contract(1, ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(entity));

        ContractResponse res = contractService.updateStatus(
                1L, new UpdateContractStatusRequest(ContractStatus.UNDER_REVIEW));

        assertThat(res.status()).isEqualTo(ContractStatus.UNDER_REVIEW);
        verify(taskService).closeContractReviewTaskOnContractStatus(1L, "UNDER_REVIEW");
    }

    @Test
    @DisplayName("updateStatus：非法流转抛 IllegalArgumentException 且不落库")
    void updateStatus_illegalTransitionThrows() {
        ContractEntity entity = contract(1, ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> contractService.updateStatus(
                1L, new UpdateContractStatusRequest(ContractStatus.SIGNED)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(contractRepository, never()).save(any());
        verify(taskService, never()).closeContractReviewTaskOnContractStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("updateStatus：UNDER_REVIEW→SIGNED 需审查通过，否则拒绝")
    void updateStatus_signRequiresApprovedReview() {
        ContractEntity notApproved = contract(1, ContractStatus.UNDER_REVIEW);
        notApproved.setReviewDecision("PENDING_CONFIRMATION");
        when(contractRepository.findById(1L)).thenReturn(Optional.of(notApproved));

        assertThatThrownBy(() -> contractService.updateStatus(
                1L, new UpdateContractStatusRequest(ContractStatus.SIGNED)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("审查");

        ContractEntity approved = contract(2, ContractStatus.UNDER_REVIEW);
        approved.setReviewDecision("APPROVED");
        when(contractRepository.findById(2L)).thenReturn(Optional.of(approved));

        ContractResponse res = contractService.updateStatus(
                2L, new UpdateContractStatusRequest(ContractStatus.SIGNED));
        assertThat(res.status()).isEqualTo(ContractStatus.SIGNED);
    }

    @Test
    @DisplayName("saveAiReview：写入审查 JSON、回到待人工确认，并把 DRAFT 推进到 UNDER_REVIEW")
    void saveAiReview_persistsJsonAndAdvancesDraft() {
        ContractEntity entity = contract(1, ContractStatus.DRAFT);
        entity.setReviewDecision("APPROVED");
        entity.setReviewerOpinion("旧意见");
        when(contractRepository.findById(1L)).thenReturn(Optional.of(entity));

        ContractReviewResponse review = new ContractReviewResponse(
                List.of(new ContractRiskItem(RiskLevel.HIGH, "违约责任", "约定不明", "建议补充")),
                List.of("缺少争议解决条款"),
                "总体风险中等",
                0.88,
                null);

        ContractResponse res = contractService.saveAiReview(1L, review);

        assertThat(res.status()).isEqualTo(ContractStatus.UNDER_REVIEW);
        assertThat(entity.getReviewRisksJson()).contains("违约责任").contains("HIGH");
        assertThat(entity.getReviewMissingClausesJson()).contains("争议解决");
        assertThat(entity.getReviewSummary()).isEqualTo("总体风险中等");
        // 新一轮 AI 审查应让旧的人工决策失效
        assertThat(entity.getReviewDecision()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(entity.getReviewerOpinion()).isEmpty();
        assertThat(entity.getReviewedAt()).isNotNull();
        assertThat(res.latestReview()).isNotNull();
        assertThat(res.latestReview().risks()).hasSize(1);
    }

    @Test
    @DisplayName("updateReview：归一化决策为大写并联动 resolve 待办")
    void updateReview_normalizesDecisionAndResolvesTask() {
        ContractEntity entity = contract(1, ContractStatus.UNDER_REVIEW);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(entity));

        ContractResponse res = contractService.updateReview(
                1L, new UpdateContractReviewRequest("同意签署", "approved"));

        assertThat(entity.getReviewDecision()).isEqualTo("APPROVED");
        assertThat(entity.getReviewedAt()).isNotNull();
        assertThat(res.latestReview().reviewerOpinion()).isEqualTo("同意签署");
        verify(taskService).resolveContractReviewTask(1L, "APPROVED");
    }

    @Test
    @DisplayName("softDelete：标记删除并落库")
    void softDelete_marksDeleted() {
        ContractEntity entity = contract(1, ContractStatus.DRAFT);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(entity));

        contractService.softDelete(1L);

        assertThat(entity.isDeleted()).isTrue();
        verify(contractRepository).save(entity);
    }

    @Test
    @DisplayName("saveAiReview：风险条目超过 8 条时截断到 8 条")
    void saveAiReview_compactsRisksToEight() {
        ContractEntity entity = contract(1, ContractStatus.UNDER_REVIEW);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(entity));

        ContractRiskItem item = new ContractRiskItem(RiskLevel.LOW, "条款", "问题", "建议");
        ContractReviewResponse review = new ContractReviewResponse(
                List.of(item, item, item, item, item, item, item, item, item, item),
                List.of(),
                "摘要",
                0.5,
                null);

        ContractResponse res = contractService.saveAiReview(1L, review);

        assertThat(res.latestReview().risks()).hasSize(8);
    }
}

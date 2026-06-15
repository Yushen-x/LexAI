package com.lexai.backend.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lexai.backend.application.dto.contract.ContractReviewRecordDetail;
import com.lexai.backend.application.dto.contract.ContractReviewRecordSummary;
import com.lexai.backend.application.dto.response.ContractReviewResponse;
import com.lexai.backend.application.dto.response.ContractRiskItem;
import com.lexai.backend.common.exception.ResourceNotFoundException;
import com.lexai.backend.domain.model.ContractStatus;
import com.lexai.backend.domain.model.RiskLevel;
import com.lexai.backend.persistence.entity.ContractEntity;
import com.lexai.backend.persistence.entity.ContractReviewRecordEntity;
import com.lexai.backend.persistence.repository.ContractRepository;
import com.lexai.backend.persistence.repository.ContractReviewRecordRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ContractReviewRecordServiceTest {

    private ContractReviewRecordRepository reviewRecordRepository;
    private ContractRepository contractRepository;
    private ContractReviewRecordService reviewRecordService;

    @BeforeEach
    void setUp() {
        reviewRecordRepository = Mockito.mock(ContractReviewRecordRepository.class);
        contractRepository = Mockito.mock(ContractRepository.class);
        reviewRecordService = new ContractReviewRecordService(
                reviewRecordRepository,
                contractRepository,
                new ObjectMapper()
        );
        when(reviewRecordRepository.save(any(ContractReviewRecordEntity.class)))
                .thenAnswer(invocation -> {
                    ContractReviewRecordEntity entity = invocation.getArgument(0);
                    if (entity.getId() == null) {
                        entity.setId(101L);
                    }
                    if (entity.getCreatedAt() == null) {
                        entity.setCreatedAt(Instant.parse("2026-06-01T01:02:03Z"));
                    }
                    if (entity.getUpdatedAt() == null) {
                        entity.setUpdatedAt(Instant.parse("2026-06-01T01:02:03Z"));
                    }
                    return entity;
                });
    }

    private ContractEntity contract(long id) {
        ContractEntity entity = new ContractEntity();
        entity.setId(id);
        entity.setContractNo("LX-2026-001");
        entity.setName("技术服务合同");
        entity.setContractType("服务合同");
        entity.setPartyA("甲方");
        entity.setPartyB("乙方");
        entity.setAmount(BigDecimal.TEN);
        entity.setContent("合同正文");
        entity.setStatus(ContractStatus.UNDER_REVIEW);
        entity.setSource("WORKSPACE_IMPORT");
        entity.setDeleted(false);
        return entity;
    }

    private ContractReviewRecordEntity reviewRecord(long id, long contractId) {
        ContractReviewRecordEntity entity = new ContractReviewRecordEntity();
        entity.setId(id);
        entity.setContractId(contractId);
        entity.setContractNo("LX-2026-001");
        entity.setContractName("技术服务合同");
        entity.setContractType("服务合同");
        entity.setReviewSummary("总体风险中等");
        entity.setReviewRisksJson("""
                [{"level":"HIGH","clause":"违约责任","issue":"约定不明","suggestion":"补充违约金基数"}]
                """);
        entity.setReviewMissingClausesJson("[\"争议解决\",\"保密条款\"]");
        entity.setReviewerOpinion("同意进入签署");
        entity.setReviewDecision("APPROVED");
        entity.setConfidence(0.87);
        entity.setSource("AI_REVIEW");
        entity.setReviewedAt(Instant.parse("2026-06-01T01:02:03Z"));
        entity.setCreatedAt(Instant.parse("2026-06-01T01:02:03Z"));
        entity.setUpdatedAt(Instant.parse("2026-06-01T02:02:03Z"));
        return entity;
    }

    @Test
    @DisplayName("recordAiReview：保存合同快照、AI 审查结果与待人工确认状态")
    void recordAiReview_persistsContractSnapshotAndReviewPayload() {
        ContractReviewResponse review = new ContractReviewResponse(
                List.of(new ContractRiskItem(RiskLevel.HIGH, "违约责任", "约定不明", "补充违约金基数")),
                List.of("争议解决"),
                "总体风险中等",
                0.92,
                null
        );

        ContractReviewRecordDetail detail = reviewRecordService.recordAiReview(contract(1L), review);

        assertThat(detail.id()).isEqualTo(101L);
        assertThat(detail.contractNo()).isEqualTo("LX-2026-001");
        assertThat(detail.summary()).isEqualTo("总体风险中等");
        assertThat(detail.risks()).hasSize(1);
        assertThat(detail.missingClauses()).containsExactly("争议解决");
        assertThat(detail.reviewDecision()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(detail.confidence()).isEqualTo(0.92);
    }

    @Test
    @DisplayName("listByContract：校验合同有效性并返回摘要统计")
    void listByContract_returnsSummaries() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract(1L)));
        when(reviewRecordRepository.findByContractIdOrderByReviewedAtDescIdDesc(1L))
                .thenReturn(List.of(reviewRecord(9L, 1L)));

        List<ContractReviewRecordSummary> summaries = reviewRecordService.listByContract(1L);

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).riskCount()).isEqualTo(1);
        assertThat(summaries.get(0).missingClauseCount()).isEqualTo(2);
        assertThat(summaries.get(0).reviewDecision()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("getByContract：返回完整风险与缺失条款")
    void getByContract_returnsDetail() {
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract(1L)));
        when(reviewRecordRepository.findByIdAndContractId(9L, 1L))
                .thenReturn(Optional.of(reviewRecord(9L, 1L)));

        ContractReviewRecordDetail detail = reviewRecordService.getByContract(1L, 9L);

        assertThat(detail.risks()).extracting(ContractRiskItem::clause)
                .containsExactly("违约责任");
        assertThat(detail.missingClauses()).containsExactly("争议解决", "保密条款");
    }

    @Test
    @DisplayName("syncLatestManualReview：仅更新最近一次历史记录的人工结论")
    void syncLatestManualReview_updatesLatestRecord() {
        ContractReviewRecordEntity latest = reviewRecord(9L, 1L);
        latest.setReviewDecision("PENDING_CONFIRMATION");
        latest.setReviewerOpinion("");
        when(reviewRecordRepository.findTopByContractIdOrderByReviewedAtDescIdDesc(1L))
                .thenReturn(Optional.of(latest));

        reviewRecordService.syncLatestManualReview(1L, "needs_revision", "请补充违约责任");

        assertThat(latest.getReviewDecision()).isEqualTo("NEEDS_REVISION");
        assertThat(latest.getReviewerOpinion()).isEqualTo("请补充违约责任");
        verify(reviewRecordRepository).save(latest);
    }

    @Test
    @DisplayName("listByContract：合同不存在或已删除时抛 ResourceNotFoundException")
    void listByContract_deletedContractThrows() {
        ContractEntity deleted = contract(1L);
        deleted.setDeleted(true);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> reviewRecordService.listByContract(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

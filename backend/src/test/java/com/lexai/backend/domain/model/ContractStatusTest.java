package com.lexai.backend.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link ContractStatus} 状态机单元测试：把 TASK.md 约定的合法/非法流转固化为用例，
 * 防止后续误改 {@code ALLOWED} 表导致合同生命周期被破坏。
 */
class ContractStatusTest {

    @ParameterizedTest(name = "{0} → {1} 应允许")
    @CsvSource({
            "DRAFT, UNDER_REVIEW",
            "DRAFT, TERMINATED",
            "UNDER_REVIEW, SIGNED",
            "UNDER_REVIEW, DRAFT",
            "UNDER_REVIEW, TERMINATED",
            "SIGNED, IN_PROGRESS",
            "SIGNED, TERMINATED",
            "IN_PROGRESS, COMPLETED",
            "IN_PROGRESS, TERMINATED"
    })
    void allowsLegalTransitions(ContractStatus from, ContractStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} → {1} 应拒绝")
    @CsvSource({
            "DRAFT, SIGNED",
            "DRAFT, IN_PROGRESS",
            "DRAFT, COMPLETED",
            "UNDER_REVIEW, IN_PROGRESS",
            "UNDER_REVIEW, COMPLETED",
            "SIGNED, DRAFT",
            "SIGNED, COMPLETED",
            "IN_PROGRESS, SIGNED"
    })
    void rejectsIllegalTransitions(ContractStatus from, ContractStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest(name = "{0} 是终态，不可再流转")
    @EnumSource(value = ContractStatus.class, names = {"COMPLETED", "TERMINATED"})
    void terminalStatesHaveNoOutgoingTransition(ContractStatus terminal) {
        for (ContractStatus target : ContractStatus.values()) {
            assertThat(terminal.canTransitionTo(target))
                    .as("%s 不应能流转到 %s", terminal, target)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("没有任何状态允许流转到自身")
    void noSelfTransition() {
        for (ContractStatus status : ContractStatus.values()) {
            assertThat(status.canTransitionTo(status))
                    .as("%s 不应能流转到自身", status)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("DRAFT 的合法目标恰为 {UNDER_REVIEW, TERMINATED}")
    void draftAllowedTargetsAreExact() {
        Set<ContractStatus> allowed = EnumSet.noneOf(ContractStatus.class);
        for (ContractStatus target : ContractStatus.values()) {
            if (ContractStatus.DRAFT.canTransitionTo(target)) {
                allowed.add(target);
            }
        }
        assertThat(allowed).containsExactlyInAnyOrder(
                ContractStatus.UNDER_REVIEW, ContractStatus.TERMINATED);
    }
}

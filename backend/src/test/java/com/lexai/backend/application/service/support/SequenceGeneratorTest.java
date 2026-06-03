package com.lexai.backend.application.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@link SequenceGenerator} 单元测试：聚焦号段解析与格式化的边界行为，
 * 确保合同号/任务号在脏数据、跨年、溢出补零等场景下都能稳定推进。
 */
class SequenceGeneratorTest {

    @Nested
    @DisplayName("buildPrefix / format")
    class PrefixAndFormat {

        @Test
        void buildPrefix_joinsCodeAndYear() {
            assertThat(SequenceGenerator.buildPrefix("LX", 2026)).isEqualTo("LX-2026-");
            assertThat(SequenceGenerator.buildPrefix("WF", 2026)).isEqualTo("WF-2026-");
        }

        @Test
        void format_padsSequenceToThreeDigits() {
            assertThat(SequenceGenerator.format("LX", 2026, 1)).isEqualTo("LX-2026-001");
            assertThat(SequenceGenerator.format("WF", 2026, 42)).isEqualTo("WF-2026-042");
        }

        @Test
        void format_keepsFullWidthWhenSequenceExceedsThreeDigits() {
            assertThat(SequenceGenerator.format("LX", 2026, 1234)).isEqualTo("LX-2026-1234");
        }

        @Test
        void buildPrefixThenFormat_areRoundTripConsistent() {
            String prefix = SequenceGenerator.buildPrefix("LX", 2026);
            String fullNo = SequenceGenerator.format("LX", 2026, 7);
            assertThat(fullNo).startsWith(prefix);
            assertThat(SequenceGenerator.nextSequence(fullNo, prefix)).isEqualTo(8L);
        }
    }

    @Nested
    @DisplayName("nextSequence")
    class NextSequence {

        @Test
        void incrementsFromCurrentMax() {
            assertThat(SequenceGenerator.nextSequence("LX-2026-001", "LX-2026-")).isEqualTo(2L);
            assertThat(SequenceGenerator.nextSequence("LX-2026-099", "LX-2026-")).isEqualTo(100L);
        }

        @ParameterizedTest
        @CsvSource({
                "LX-2026-007, LX-2026-, 8",
                "WF-2026-000, WF-2026-, 1",
                "LX-2026-1000, LX-2026-, 1001"
        })
        void parsesNumericSuffix(String currentNo, String prefix, long expected) {
            assertThat(SequenceGenerator.nextSequence(currentNo, prefix)).isEqualTo(expected);
        }

        @Test
        void returnsOneWhenCurrentNoIsNull() {
            assertThat(SequenceGenerator.nextSequence(null, "LX-2026-")).isEqualTo(1L);
        }

        @Test
        void returnsOneWhenPrefixDoesNotMatch() {
            // 跨年：去年的号不应被今年的前缀续上
            assertThat(SequenceGenerator.nextSequence("LX-2025-050", "LX-2026-")).isEqualTo(1L);
        }

        @ParameterizedTest
        @ValueSource(strings = {"LX-2026-abc", "LX-2026-", "LX-2026-12x"})
        void returnsOneWhenSuffixNotParseable(String currentNo) {
            assertThat(SequenceGenerator.nextSequence(currentNo, "LX-2026-")).isEqualTo(1L);
        }
    }
}

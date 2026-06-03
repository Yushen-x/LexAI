package com.lexai.backend.application.service.support;

import java.time.Year;

/**
 * 业务流水号（合同号 {@code LX-2026-001}、任务号 {@code WF-2026-001} 等）的统一生成工具。
 *
 * <p>此前 {@code ContractService} 与 {@code TaskService} 各自维护了一份几乎相同的
 * {@code nextSequence(...)} 与号段格式化逻辑，容易出现两边规则漂移。这里抽取为无状态工具，
 * 统一「前缀拼接 → 解析当前最大号 → 自增 → 补零格式化」四步，便于单元测试覆盖边界。</p>
 *
 * <p>设计取舍：保持纯函数、不持有可变状态，因此线程安全；并发下的「号码不重复」仍由调用方
 * 的事务/锁或数据库唯一约束保证，本工具只负责确定性地算出「下一个号」。</p>
 */
public final class SequenceGenerator {

    private SequenceGenerator() {
    }

    /**
     * 构造用于「查当前最大号」的检索前缀，例如 {@code buildPrefix("LX", 2026)} → {@code "LX-2026-"}。
     */
    public static String buildPrefix(String code, int year) {
        return code + "-" + year + "-";
    }

    /**
     * 用当前年份构造检索前缀，等价于 {@code buildPrefix(code, Year.now().getValue())}。
     */
    public static String buildPrefixForCurrentYear(String code) {
        return buildPrefix(code, Year.now().getValue());
    }

    /**
     * 把序号格式化为完整业务号，例如 {@code format("LX", 2026, 1)} → {@code "LX-2026-001"}。
     */
    public static String format(String code, int year, long sequence) {
        return String.format("%s-%d-%03d", code, year, sequence);
    }

    /**
     * 根据「当前同前缀的最大号」推算下一个序号。
     *
     * <p>当 {@code currentNo} 为空、前缀不匹配或尾段无法解析为数字时，安全回退为 {@code 1}，
     * 保证新一年的第一条记录从 001 起算，也不会因脏数据抛异常中断创建流程。</p>
     *
     * @param currentNo 当前已存在的最大号（可为 {@code null}）
     * @param prefix    号段前缀，通常来自 {@link #buildPrefix(String, int)}
     * @return 下一个序号，最小为 {@code 1}
     */
    public static long nextSequence(String currentNo, String prefix) {
        if (currentNo == null || !currentNo.startsWith(prefix)) {
            return 1L;
        }
        String suffix = currentNo.substring(prefix.length());
        try {
            return Long.parseLong(suffix) + 1;
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }
}

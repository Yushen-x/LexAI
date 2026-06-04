package com.lexai.backend.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.cache.annotation.CacheEvict;

/**
 * 组合注解：标记会改变合同统计口径的写操作，触发清空全部合同统计缓存。
 *
 * <p>把分散在多个写方法上的 {@link CacheEvict} 列表收敛到一处，避免缓存名遗漏或漂移。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@CacheEvict(cacheNames = {
        CacheConfig.CONTRACT_STATS_SUMMARY,
        CacheConfig.CONTRACT_STATUS_DISTRIBUTION,
        CacheConfig.CONTRACT_TYPE_DISTRIBUTION,
        CacheConfig.CONTRACT_MONTHLY_TREND
}, allEntries = true)
public @interface EvictContractStatsCaches {
}

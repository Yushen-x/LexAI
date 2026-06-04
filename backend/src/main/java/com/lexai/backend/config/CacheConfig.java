package com.lexai.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 进程内缓存配置（Caffeine）。
 *
 * <p>合同统计类查询读多写少、计算需聚合多张/多次查询，适合短 TTL 缓存：
 * 既显著降低重复聚合开销，又通过「写操作主动失效 + 过期兜底」把数据陈旧窗口控制在秒级。</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 合同统计相关缓存名，写操作据此失效。 */
    public static final String CONTRACT_STATS_SUMMARY = "contractStatsSummary";
    public static final String CONTRACT_STATUS_DISTRIBUTION = "contractStatusDistribution";
    public static final String CONTRACT_TYPE_DISTRIBUTION = "contractTypeDistribution";
    public static final String CONTRACT_MONTHLY_TREND = "contractMonthlyTrend";

    /** 所有合同统计缓存名，便于一次性失效。 */
    public static final String[] CONTRACT_STATS_CACHES = {
            CONTRACT_STATS_SUMMARY,
            CONTRACT_STATUS_DISTRIBUTION,
            CONTRACT_TYPE_DISTRIBUTION,
            CONTRACT_MONTHLY_TREND
    };

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(CONTRACT_STATS_CACHES);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(60))
                .maximumSize(500));
        return manager;
    }
}

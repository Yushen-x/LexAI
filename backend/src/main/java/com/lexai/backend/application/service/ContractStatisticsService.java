package com.lexai.backend.application.service;

import com.lexai.backend.config.CacheConfig;
import com.lexai.backend.persistence.repository.ContractRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 合同统计聚合服务。各统计口径独立暴露为接口，均做短 TTL 缓存（见 {@link CacheConfig}），
 * 合同写操作会主动失效这些缓存。注意 {@link #summary()} 内部直接调用其它方法属自调用，
 * 不经缓存代理；其整体结果由 {@code summary} 自身的缓存兜底，故无需依赖子方法缓存生效。
 */
@Service
public class ContractStatisticsService {

    private final ContractRepository contractRepository;

    public ContractStatisticsService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Cacheable(CacheConfig.CONTRACT_STATS_SUMMARY)
    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", contractRepository.countByDeletedFalse());
        result.put("statusDistribution", statusDistribution());
        result.put("typeDistribution", typeDistribution());
        result.put("monthlyTrend", monthlyTrend());
        return result;
    }

    @Cacheable(CacheConfig.CONTRACT_STATUS_DISTRIBUTION)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> statusDistribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : contractRepository.countGroupByStatus()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("status", row[0] != null ? row[0].toString() : "UNKNOWN");
            item.put("count", row[1]);
            list.add(item);
        }
        return list;
    }

    @Cacheable(CacheConfig.CONTRACT_TYPE_DISTRIBUTION)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> typeDistribution() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : contractRepository.countGroupByType()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", row[0] != null ? row[0].toString() : "其他");
            item.put("count", row[1]);
            list.add(item);
        }
        return list;
    }

    @Cacheable(CacheConfig.CONTRACT_MONTHLY_TREND)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> monthlyTrend() {
        Instant since = Instant.now().minus(180, ChronoUnit.DAYS);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : contractRepository.monthlyCreation(since)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("year", row[0]);
            item.put("month", row[1]);
            item.put("count", row[2]);
            list.add(item);
        }
        return list;
    }
}

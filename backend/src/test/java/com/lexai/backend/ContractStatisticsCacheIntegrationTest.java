package com.lexai.backend;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.lexai.backend.application.dto.contract.CreateContractRequest;
import com.lexai.backend.application.service.ContractService;
import com.lexai.backend.application.service.ContractStatisticsService;
import com.lexai.backend.persistence.repository.ContractRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;

/**
 * 校验合同统计缓存：相同读取命中缓存只查库一次；发生合同写操作后缓存失效、重新聚合。
 */
@SpringBootTest
class ContractStatisticsCacheIntegrationTest {

    @SpyBean
    private ContractRepository contractRepository;

    @Autowired
    private ContractStatisticsService statisticsService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    @DisplayName("summary 连续读取命中缓存，仅查库一次；合同写操作后失效并重新聚合")
    void summaryIsCachedUntilContractMutation() {
        statisticsService.summary();
        statisticsService.summary();
        // 两次读取只触发一次底层聚合查询
        verify(contractRepository, times(1)).countByDeletedFalse();

        // 写操作触发 @EvictContractStatsCaches，缓存失效
        contractService.create(new CreateContractRequest(
                "缓存校验合同", "服务", null, null, null, null, null, null));

        statisticsService.summary();
        // 失效后重新聚合，底层查询再次被调用
        verify(contractRepository, times(2)).countByDeletedFalse();
    }
}

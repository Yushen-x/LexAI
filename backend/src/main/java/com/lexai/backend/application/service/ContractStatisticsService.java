package com.lexai.backend.application.service;

import com.lexai.backend.persistence.repository.ContractRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractStatisticsService {

    private final ContractRepository contractRepository;

    public ContractStatisticsService(ContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", contractRepository.countByDeletedFalse());
        result.put("statusDistribution", statusDistribution());
        result.put("typeDistribution", typeDistribution());
        result.put("monthlyTrend", monthlyTrend());
        return result;
    }

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

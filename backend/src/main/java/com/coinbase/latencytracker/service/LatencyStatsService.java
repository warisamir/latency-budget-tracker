package com.coinbase.latencytracker.service;

import com.coinbase.latencytracker.dto.LatencyStatsDto;
import com.coinbase.latencytracker.dto.LatencyStatsDto.StageStats;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.config.LatencyBudgetProperties;
import com.coinbase.latencytracker.repository.LatencyRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LatencyStatsService {

    private final LatencyRecordRepository  recordRepository;
    private final LatencyBudgetProperties  props;

    public LatencyStatsDto getStats(String window) {
        Instant since = windowToInstant(window);
        List<StageStats> stageStats = new ArrayList<>();

        for (Stage stage : Stage.values()) {
            long budget = props.getBudget().getAuthentication(); // overridden below
            budget = switch (stage) {
                case AUTHENTICATION       -> props.getBudget().getAuthentication();
                case VALIDATION           -> props.getBudget().getValidation();
                case BUSINESS_LOGIC       -> props.getBudget().getBusinessLogic();
                case CACHE_ACCESS         -> props.getBudget().getCacheAccess();
                case DATABASE_QUERY       -> props.getBudget().getDatabaseQuery();
                case RESPONSE_SERIALIZATION -> props.getBudget().getResponseSerialization();
            };

            double p50 = recordRepository.findP50ByStageAndCreatedAtAfter(stage.name(), since).orElse(0.0);
            double p95 = recordRepository.findP95ByStageAndCreatedAtAfter(stage.name(), since).orElse(0.0);
            double p99 = recordRepository.findP99ByStageAndCreatedAtAfter(stage.name(), since).orElse(0.0);
            double avg = recordRepository.findAvgByStageAndCreatedAtAfter(stage, since).orElse(0.0);
            long violations = recordRepository.countBudgetExceededByStageAndCreatedAtAfter(stage, since);

            // total count for this stage
            long total = recordRepository.findByStageAndCreatedAtAfter(stage, since).size();
            double rate = total > 0 ? (double) violations / total * 100 : 0;

            stageStats.add(StageStats.builder()
                    .stage(stage)
                    .p50Ms(round(p50))
                    .p95Ms(round(p95))
                    .p99Ms(round(p99))
                    .avgMs(round(avg))
                    .budgetMs(budget)
                    .violations(violations)
                    .violationRate(round(rate))
                    .healthy(p95 <= budget)
                    .build());
        }

        long totalRequests  = recordRepository.countByCreatedAtAfter(since);
        long totalViolations = stageStats.stream().mapToLong(StageStats::getViolations).sum();
        double violationRate = totalRequests > 0 ? (double) totalViolations / totalRequests * 100 : 0;

        return LatencyStatsDto.builder()
                .generatedAt(Instant.now())
                .window(window)
                .totalRequests(totalRequests)
                .budgetViolations(totalViolations)
                .violationRate(round(violationRate))
                .stageStats(stageStats)
                .build();
    }

    private Instant windowToInstant(String window) {
        return switch (window.toLowerCase()) {
            case "1h"  -> Instant.now().minus(1,  ChronoUnit.HOURS);
            case "24h" -> Instant.now().minus(24, ChronoUnit.HOURS);
            case "7d"  -> Instant.now().minus(7,  ChronoUnit.DAYS);
            default    -> Instant.now().minus(1,  ChronoUnit.HOURS);
        };
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}

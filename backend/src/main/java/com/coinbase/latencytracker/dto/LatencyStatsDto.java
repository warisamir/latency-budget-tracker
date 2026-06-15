package com.coinbase.latencytracker.dto;

import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatencyStatsDto {

    private Instant generatedAt;
    private String window;  // "1h", "24h", "7d"
    private long totalRequests;
    private long budgetViolations;
    private double violationRate;
    private List<StageStats> stageStats;
    private Map<String, Long> severityCounts;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StageStats {
        private Stage stage;
        private double p50Ms;
        private double p95Ms;
        private double p99Ms;
        private double avgMs;
        private long budgetMs;
        private long violations;
        private double violationRate;
        private boolean healthy;
    }
}

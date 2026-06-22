package com.coinbase.latencytracker.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatencyHistoryDto {

    private String window;  // "1h", "24h", "7d"
    private int buckets;
    private long bucketDurationMs;
    private List<DataPoint> dataPoints;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DataPoint {
        private Instant timestamp;
        private long totalRequests;
        private long violations;
        private double violationRate;
        private double avgLatencyMs;
    }
}

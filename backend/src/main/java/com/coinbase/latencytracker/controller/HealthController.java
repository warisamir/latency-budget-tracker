package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.tracing.TracingService;
import com.coinbase.latencytracker.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final DataSource                    dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TracingService                tracingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        long start = System.currentTimeMillis();
        LoggerUtil.debug(log, "Health check initiated");

        Map<String, Object> dbStatus = checkDatabase();
        Map<String, Object> redisStatus = checkRedis();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", Instant.now());
        result.put("traceId", tracingService.currentTraceId());
        result.put("components", Map.of(
                "database", dbStatus,
                "redis",    redisStatus
        ));

        LoggerUtil.logPerformance(log, "health", System.currentTimeMillis() - start);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        LoggerUtil.debug(log, "Liveness probe requested");
        return ResponseEntity.ok(Map.of("status", "ALIVE"));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        long start = System.currentTimeMillis();
        LoggerUtil.debug(log, "Readiness probe requested");

        boolean dbOk    = "UP".equals(checkDatabase().get("status"));
        boolean redisOk = "UP".equals(checkRedis().get("status"));
        boolean ready   = dbOk && redisOk;

        Map<String, Object> result = Map.of(
                "status", ready ? "READY" : "NOT_READY",
                "database", dbOk,
                "redis", redisOk
        );

        if (!ready) {
            LoggerUtil.warn(log, "Readiness check failed - database={}, redis={}", dbOk, redisOk);
        }

        LoggerUtil.logPerformance(log, "readiness", System.currentTimeMillis() - start);
        return ready ? ResponseEntity.ok(result) : ResponseEntity.status(503).body(result);
    }

    private Map<String, Object> checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            if (!valid) {
                LoggerUtil.warn(log, "Database validation failed");
            }
            return Map.of("status", valid ? "UP" : "DOWN");
        } catch (Exception e) {
            LoggerUtil.error(log, "Database health check failed", e);
            return Map.of("status", "DOWN", "error", e.getClass().getSimpleName());
        }
    }

    private Map<String, Object> checkRedis() {
        try {
            redisTemplate.opsForValue().get("__health_check__");
            return Map.of("status", "UP");
        } catch (Exception e) {
            LoggerUtil.error(log, "Redis health check failed", e);
            return Map.of("status", "DOWN", "error", e.getClass().getSimpleName());
        }
    }
}

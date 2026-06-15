package com.coinbase.latencytracker.controller;

import com.coinbase.latencytracker.tracing.TracingService;
import lombok.RequiredArgsConstructor;
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
public class HealthController {

    private final DataSource                    dataSource;
    private final RedisTemplate<String, Object> redisTemplate;
    private final TracingService                tracingService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", Instant.now());
        result.put("traceId", tracingService.currentTraceId());
        result.put("components", Map.of(
                "database", checkDatabase(),
                "redis",    checkRedis()
        ));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "ALIVE"));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        boolean dbOk    = "UP".equals(checkDatabase().get("status"));
        boolean redisOk = "UP".equals(checkRedis().get("status"));
        boolean ready   = dbOk && redisOk;

        Map<String, Object> result = Map.of(
                "status", ready ? "READY" : "NOT_READY",
                "database", dbOk,
                "redis", redisOk
        );
        return ready ? ResponseEntity.ok(result) : ResponseEntity.status(503).body(result);
    }

    private Map<String, Object> checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(2);
            return Map.of("status", valid ? "UP" : "DOWN");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkRedis() {
        try {
            redisTemplate.opsForValue().get("__health_check__");
            return Map.of("status", "UP");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "error", e.getMessage());
        }
    }
}

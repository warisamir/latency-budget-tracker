package com.coinbase.latencytracker.repository;

import com.coinbase.latencytracker.entity.LatencyRecord;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LatencyRecordRepository extends JpaRepository<LatencyRecord, Long> {

    List<LatencyRecord> findByTransactionId(String transactionId);

    Page<LatencyRecord> findByStage(Stage stage, Pageable pageable);

    Page<LatencyRecord> findByBudgetExceededTrue(Pageable pageable);

    @Query("SELECT r FROM LatencyRecord r WHERE r.stage = :stage AND r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<LatencyRecord> findByStageAndCreatedAtAfter(@Param("stage") Stage stage,
                                                     @Param("since") Instant since);

    @Query("SELECT r FROM LatencyRecord r WHERE r.createdAt >= :since ORDER BY r.createdAt DESC")
    List<LatencyRecord> findAllSince(@Param("since") Instant since);

    /**
     * Compute P95 using a native percentile_cont query (PostgreSQL).
     * Falls back gracefully in H2 for unit tests.
     */
    @Query(value = """
            SELECT PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY actual_latency_ms)
            FROM latency_records
            WHERE stage = :stage AND created_at >= :since
            """, nativeQuery = true)
    Optional<Double> findP95ByStageAndCreatedAtAfter(@Param("stage") String stage,
                                                     @Param("since") Instant since);

    @Query(value = """
            SELECT PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY actual_latency_ms)
            FROM latency_records
            WHERE stage = :stage AND created_at >= :since
            """, nativeQuery = true)
    Optional<Double> findP99ByStageAndCreatedAtAfter(@Param("stage") String stage,
                                                     @Param("since") Instant since);

    @Query(value = """
            SELECT PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY actual_latency_ms)
            FROM latency_records
            WHERE stage = :stage AND created_at >= :since
            """, nativeQuery = true)
    Optional<Double> findP50ByStageAndCreatedAtAfter(@Param("stage") String stage,
                                                     @Param("since") Instant since);

    @Query("SELECT AVG(r.actualLatencyMs) FROM LatencyRecord r WHERE r.stage = :stage AND r.createdAt >= :since")
    Optional<Double> findAvgByStageAndCreatedAtAfter(@Param("stage") Stage stage,
                                                     @Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM LatencyRecord r WHERE r.stage = :stage AND r.budgetExceeded = true AND r.createdAt >= :since")
    long countBudgetExceededByStageAndCreatedAtAfter(@Param("stage") Stage stage,
                                                     @Param("since") Instant since);

    @Query("SELECT COUNT(r) FROM LatencyRecord r WHERE r.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") Instant since);

    @Query("SELECT r.stage, AVG(r.actualLatencyMs) FROM LatencyRecord r WHERE r.createdAt >= :since GROUP BY r.stage")
    List<Object[]> findAvgLatencyByStageAfter(@Param("since") Instant since);

    @Query(value = """
            SELECT DATE_TRUNC(:bucket, created_at) as bucket_time,
                   COUNT(*) as total_requests,
                   SUM(CASE WHEN budget_exceeded = true THEN 1 ELSE 0 END) as violations,
                   AVG(actual_latency_ms) as avg_latency
            FROM latency_records
            WHERE created_at >= :since
            GROUP BY DATE_TRUNC(:bucket, created_at)
            ORDER BY bucket_time ASC
            """, nativeQuery = true)
    List<Object[]> findHistoryByWindow(@Param("since") Instant since, @Param("bucket") String bucket);
}

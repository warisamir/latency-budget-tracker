package com.coinbase.latencytracker.repository;

import com.coinbase.latencytracker.entity.Alert;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
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
public interface AlertRepository extends JpaRepository<Alert, Long> {

    Page<Alert> findByResolvedFalse(Pageable pageable);

    List<Alert> findByTransactionId(String transactionId);

    /**
     * Deduplication: check if an active (unresolved) alert for this stage
     * was created within the cooldown window.
     */
    @Query("""
            SELECT a FROM Alert a
            WHERE a.stage = :stage
              AND a.resolved = false
              AND a.createdAt >= :since
            ORDER BY a.createdAt DESC
            """)
    List<Alert> findActiveAlertsForStageSince(@Param("stage") Stage stage,
                                              @Param("since") Instant since);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.resolved = false AND a.createdAt >= :since")
    long countActiveAlertsSince(@Param("since") Instant since);

    Page<Alert> findBySeverityAndResolvedFalse(Severity severity, Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.resolved = false AND a.severity IN ('HIGH','CRITICAL') ORDER BY a.createdAt DESC")
    List<Alert> findCriticalActiveAlerts();

    Optional<Alert> findTopByStageAndResolvedFalseOrderByCreatedAtDesc(Stage stage);
}

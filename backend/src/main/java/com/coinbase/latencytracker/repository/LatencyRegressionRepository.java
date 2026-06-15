package com.coinbase.latencytracker.repository;

import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import com.coinbase.latencytracker.entity.LatencyRegression;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LatencyRegressionRepository extends JpaRepository<LatencyRegression, Long> {

    List<LatencyRegression> findByStageOrderByDetectedAtDesc(Stage stage);

    Page<LatencyRegression> findByDetectedAtAfterOrderByDetectedAtDesc(Instant since, Pageable pageable);
}

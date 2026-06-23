package com.coinbase.latencytracker.event;

import com.coinbase.latencytracker.service.LatencyRecordBatchWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Startup listener to initialize async batch writer.
 * Starts the background batch flush loop on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationStartupListener {

    private final LatencyRecordBatchWriter batchWriter;

    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationStart() {
        log.info("Starting LatencyRecordBatchWriter background thread");
        batchWriter.startBatchFlushLoop();
    }
}

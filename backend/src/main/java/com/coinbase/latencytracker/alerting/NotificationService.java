package com.coinbase.latencytracker.alerting;

import com.coinbase.latencytracker.entity.Alert;
import com.coinbase.latencytracker.entity.LatencyRecord.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Notification dispatcher.
 *
 * In production: inject Slack WebClient, JavaMailSender, PagerDuty client, etc.
 * Here we structured-log every alert so it can be scraped by Loki/Fluentd.
 */
@Service
@Slf4j
public class NotificationService {

    public void sendAlert(Alert alert) {
        sendSlack(alert);
        if (isCritical(alert)) {
            sendEmail(alert);
        }
    }

    private void sendSlack(Alert alert) {
        // TODO: inject SlackClient and call chat.postMessage
        log.warn("[SLACK ALERT] id={} stage={} severity={} actual={}ms budget={}ms deviation={}% traceId={}",
                alert.getId(),
                alert.getStage(),
                alert.getSeverity(),
                alert.getActualLatencyMs(),
                alert.getBudgetMs(),
                String.format("%.1f", alert.getDeviationPercent()),
                alert.getTraceId());
    }

    private void sendEmail(Alert alert) {
        // TODO: inject JavaMailSender
        log.warn("[EMAIL ALERT] CRITICAL latency alert — stage={} traceId={}",
                alert.getStage(), alert.getTraceId());
    }

    private boolean isCritical(Alert alert) {
        return alert.getSeverity() == Severity.CRITICAL
            || alert.getSeverity() == Severity.HIGH;
    }
}

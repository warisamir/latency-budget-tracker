package com.coinbase.latencytracker.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "latency")
@Getter @Setter
public class LatencyBudgetProperties {

    private Budget budget = new Budget();
    private Alerting alerting = new Alerting();
    private Regression regression = new Regression();

    @Getter @Setter
    public static class Budget {
        private long authentication = 20;
        private long validation = 10;
        private long businessLogic = 50;
        private long cacheAccess = 10;
        private long databaseQuery = 40;
        private long responseSerialization = 5;
        private long total = 135;
    }

    @Getter @Setter
    public static class Alerting {
        private long cooldownMinutes = 5;
        private long deduplicationWindowMinutes = 10;
    }

    @Getter @Setter
    public static class Regression {
        private long checkIntervalMs = 300_000;
        private Thresholds thresholds = new Thresholds();

        @Getter @Setter
        public static class Thresholds {
            private double low = 5.0;
            private double medium = 10.0;
            private double high = 20.0;
            private double critical = 50.0;
        }
    }
}

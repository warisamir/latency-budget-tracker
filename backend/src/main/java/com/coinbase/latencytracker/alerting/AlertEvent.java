package com.coinbase.latencytracker.alerting;

import com.coinbase.latencytracker.budget.BudgetResult;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent published whenever a stage exceeds its budget.
 */
@Getter
public class AlertEvent extends ApplicationEvent {

    private final String      transactionId;
    private final String      traceId;
    private final BudgetResult budgetResult;

    public AlertEvent(Object source, String transactionId, String traceId, BudgetResult budgetResult) {
        super(source);
        this.transactionId = transactionId;
        this.traceId       = traceId;
        this.budgetResult  = budgetResult;
    }
}

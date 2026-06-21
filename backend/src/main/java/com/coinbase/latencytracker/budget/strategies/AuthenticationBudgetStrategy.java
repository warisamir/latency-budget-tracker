package com.coinbase.latencytracker.budget.strategies;

import com.coinbase.latencytracker.budget.AbstractBudgetStrategy;
import com.coinbase.latencytracker.config.LatencyBudgetProperties;
import com.coinbase.latencytracker.entity.LatencyRecord.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationBudgetStrategy extends AbstractBudgetStrategy {

    private final LatencyBudgetProperties props;

    @Override public Stage getStage()    { return Stage.AUTHENTICATION; }
    @Override public long  getBudgetMs() { return props.getBudget().getAuthentication(); }
}

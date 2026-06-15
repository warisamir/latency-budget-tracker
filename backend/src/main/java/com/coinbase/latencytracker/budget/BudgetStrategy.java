package com.coinbase.latencytracker.budget;

import com.coinbase.latencytracker.entity.LatencyRecord.Stage;

/**
 * Strategy contract: each stage has its own budget evaluation strategy.
 * Implement this interface to customise how a particular stage is judged.
 */
public interface BudgetStrategy {

    /** The stage this strategy handles. */
    Stage getStage();

    /** Budget in milliseconds for this stage. */
    long getBudgetMs();

    /**
     * Evaluate the actual latency and produce a BudgetResult.
     * Implementations must be stateless and thread-safe.
     */
    BudgetResult evaluate(long actualMs);
}

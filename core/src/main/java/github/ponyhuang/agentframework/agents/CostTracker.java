package github.ponyhuang.agentframework.agents;

import java.util.concurrent.atomic.AtomicReference;

/**
 * CostTracker - Tracks cumulative costs across agent execution sessions.
 * Used to enforce max_budget_usd limits.
 */
public class CostTracker {

    private final double maxBudgetUsd;
    private final AtomicReference<Double> totalCostUsd = new AtomicReference<>(0.0);

    /**
     * Create a CostTracker with unlimited budget.
     */
    public CostTracker() {
        this(Double.MAX_VALUE);
    }

    /**
     * Create a CostTracker with a specific budget limit.
     *
     * @param maxBudgetUsd the maximum budget in USD
     */
    public CostTracker(double maxBudgetUsd) {
        this.maxBudgetUsd = maxBudgetUsd > 0 ? maxBudgetUsd : Double.MAX_VALUE;
    }

    /**
     * Add a cost to the tracker.
     *
     * @param costUsd the cost to add in USD
     */
    public void addCost(double costUsd) {
        if (costUsd > 0) {
            totalCostUsd.updateAndGet(current -> current + costUsd);
        }
    }

    /**
     * Check if the budget has been exceeded.
     *
     * @return true if budget exceeded, false otherwise
     */
    public boolean isBudgetExceeded() {
        return totalCostUsd.get() >= maxBudgetUsd;
    }

    /**
     * Get the remaining budget.
     *
     * @return remaining budget in USD
     */
    public double getRemainingBudget() {
        double current = totalCostUsd.get();
        if (current >= maxBudgetUsd) {
            return 0.0;
        }
        return maxBudgetUsd - current;
    }

    /**
     * Get the total cost so far.
     *
     * @return total cost in USD
     */
    public double getTotalCostUsd() {
        return totalCostUsd.get();
    }

    /**
     * Get the max budget.
     *
     * @return max budget in USD
     */
    public double getMaxBudgetUsd() {
        return maxBudgetUsd == Double.MAX_VALUE ? 0.0 : maxBudgetUsd;
    }

    /**
     * Check if budget is unlimited.
     *
     * @return true if unlimited budget
     */
    public boolean isUnlimited() {
        return maxBudgetUsd == Double.MAX_VALUE;
    }

    /**
     * Reset the cost tracker.
     */
    public void reset() {
        totalCostUsd.set(0.0);
    }
}

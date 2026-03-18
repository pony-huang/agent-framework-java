package github.ponyhuang.agentframework.agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CostTracker.
 */
class CostTrackerTest {

    @Test
    void testUnlimitedBudget() {
        CostTracker tracker = new CostTracker();

        assertTrue(tracker.isUnlimited());
        assertFalse(tracker.isBudgetExceeded());
        assertEquals(0.0, tracker.getTotalCostUsd());
        assertEquals(0.0, tracker.getMaxBudgetUsd());
    }

    @Test
    void testBudgetLimit() {
        CostTracker tracker = new CostTracker(1.0);

        assertFalse(tracker.isUnlimited());
        assertEquals(1.0, tracker.getMaxBudgetUsd());
        assertFalse(tracker.isBudgetExceeded());
    }

    @Test
    void testAddCost() {
        CostTracker tracker = new CostTracker(1.0);

        tracker.addCost(0.5);
        assertEquals(0.5, tracker.getTotalCostUsd(), 0.001);
        assertFalse(tracker.isBudgetExceeded());

        tracker.addCost(0.3);
        assertEquals(0.8, tracker.getTotalCostUsd(), 0.001);
        assertFalse(tracker.isBudgetExceeded());

        tracker.addCost(0.3);
        assertEquals(1.1, tracker.getTotalCostUsd(), 0.001);
        assertTrue(tracker.isBudgetExceeded());
    }

    @Test
    void testRemainingBudget() {
        CostTracker tracker = new CostTracker(1.0);

        assertEquals(1.0, tracker.getRemainingBudget(), 0.001);

        tracker.addCost(0.4);
        assertEquals(0.6, tracker.getRemainingBudget(), 0.001);

        tracker.addCost(0.7);
        assertEquals(0.0, tracker.getRemainingBudget(), 0.001);
    }

    @Test
    void testNegativeCostIgnored() {
        CostTracker tracker = new CostTracker(1.0);

        tracker.addCost(-0.5);
        assertEquals(0.0, tracker.getTotalCostUsd(), 0.001);
    }

    @Test
    void testZeroCostIgnored() {
        CostTracker tracker = new CostTracker(1.0);

        tracker.addCost(0.0);
        assertEquals(0.0, tracker.getTotalCostUsd(), 0.001);
    }

    @Test
    void testReset() {
        CostTracker tracker = new CostTracker(1.0);

        tracker.addCost(0.8);
        assertEquals(0.8, tracker.getTotalCostUsd(), 0.001);

        tracker.reset();
        assertEquals(0.0, tracker.getTotalCostUsd(), 0.001);
        assertFalse(tracker.isBudgetExceeded());
    }

    @Test
    void testZeroBudgetMeansUnlimited() {
        CostTracker tracker = new CostTracker(0.0);
        assertTrue(tracker.isUnlimited());

        tracker = new CostTracker(-1.0);
        assertTrue(tracker.isUnlimited());
    }

    @Test
    void testExactBudgetExceeded() {
        CostTracker tracker = new CostTracker(1.0);

        tracker.addCost(1.0);
        assertTrue(tracker.isBudgetExceeded());
    }
}

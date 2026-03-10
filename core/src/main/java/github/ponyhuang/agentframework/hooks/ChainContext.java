package github.ponyhuang.agentframework.hooks;

/**
 * Context passed through the hook execution chain.
 * Tracks accumulated results and chain state.
 */
public class ChainContext {

    private HookResult accumulatedResult;
    private boolean chainStopped;
    private int currentIndex;
    private String matcherValue;

    public ChainContext() {
        this.accumulatedResult = HookResult.allow();
        this.chainStopped = false;
        this.currentIndex = 0;
    }

    /**
     * Creates a ChainContext with initial matcher value for filtering.
     *
     * @param matcherValue the value to match against
     */
    public ChainContext(String matcherValue) {
        this();
        this.matcherValue = matcherValue;
    }

    /**
     * Accumulates a hook result into the chain.
     *
     * @param result the result to accumulate
     */
    public void accumulate(HookResult result) {
        if (result == null) {
            return;
        }

        // If result is deny, stop the chain
        if (!result.isAllow()) {
            this.chainStopped = true;
            if (result.getReason() != null) {
                this.accumulatedResult.setReason(result.getReason());
            }
        }

        // If should not continue, stop the chain
        if (!result.isShouldContinue()) {
            this.chainStopped = true;
            if (result.getStopReason() != null) {
                this.accumulatedResult.setStopReason(result.getStopReason());
            }
        }

        // Aggregate additional context
        if (result.getAdditionalContext() != null && !result.getAdditionalContext().isEmpty()) {
            String existing = this.accumulatedResult.getAdditionalContext();
            if (existing == null || existing.isEmpty()) {
                this.accumulatedResult.setAdditionalContext(result.getAdditionalContext());
            } else {
                this.accumulatedResult.setAdditionalContext(existing + "\n" + result.getAdditionalContext());
            }
        }

        // Apply hook-specific output (last one wins)
        if (result.getHookSpecificOutput() != null) {
            this.accumulatedResult.setHookSpecificOutput(result.getHookSpecificOutput());
        }

        // Apply system message
        if (result.getSystemMessage() != null) {
            this.accumulatedResult.setSystemMessage(result.getSystemMessage());
        }

        // Apply suppress output
        if (result.isSuppressOutput()) {
            this.accumulatedResult.setSuppressOutput(true);
        }
    }

    /**
     * Stops the execution chain.
     */
    public void stop() {
        this.chainStopped = true;
    }

    /**
     * Stops the chain with a reason.
     *
     * @param reason the reason for stopping
     */
    public void stop(String reason) {
        this.chainStopped = true;
        this.accumulatedResult.setAllow(false);
        this.accumulatedResult.setReason(reason);
    }

    /**
     * Moves to the next observer in the chain.
     */
    public void next() {
        this.currentIndex++;
    }

    // Getters and setters

    public HookResult getAccumulatedResult() {
        return accumulatedResult;
    }

    public void setAccumulatedResult(HookResult accumulatedResult) {
        this.accumulatedResult = accumulatedResult;
    }

    public boolean isChainStopped() {
        return chainStopped;
    }

    public void setChainStopped(boolean chainStopped) {
        this.chainStopped = chainStopped;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int currentIndex) {
        this.currentIndex = currentIndex;
    }

    public String getMatcherValue() {
        return matcherValue;
    }

    public void setMatcherValue(String matcherValue) {
        this.matcherValue = matcherValue;
    }

    /**
     * Checks if the chain should continue to the next observer.
     *
     * @return true if should continue, false otherwise
     */
    public boolean shouldContinue() {
        return !chainStopped && accumulatedResult.isAllow();
    }
}
package github.ponyhuang.agentframework.samples.validation;

import java.time.Duration;

/**
 * Result of a sample execution.
 */
public class SampleResult {
    private final String sampleName;
    private final boolean success;
    private final String output;
    private final String error;
    private final Duration duration;

    public SampleResult(String sampleName, boolean success, String output, String error, Duration duration) {
        this.sampleName = sampleName;
        this.success = success;
        this.output = output;
        this.error = error;
        this.duration = duration;
    }

    public String getSampleName() {
        return sampleName;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }

    public Duration getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%d ms)", 
                success ? "PASS" : "FAIL", 
                sampleName, 
                duration.toMillis());
    }
}

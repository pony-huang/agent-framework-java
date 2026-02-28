package github.ponyhuang.agentframework.samples;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Validates that all samples run without error.
 * Requires valid API keys in environment variables.
 */
public class SamplesTest {

    private void runSample(Runnable runnable) {
        // Skip test if API keys are not present
        Assumptions.assumeTrue(
            System.getenv("MY_OPENAI_API_KEY") != null || 
            System.getenv("MY_ANTHROPIC_AUTH_TOKEN") != null,
            "Skipping sample test because API keys are missing."
        );

        // Run with timeout to prevent hanging
        assertTimeoutPreemptively(Duration.ofMinutes(2), runnable::run);
    }

    @Test
    void testSimpleAgentExample() {
        runSample(() -> SimpleAgentExample.main(new String[]{}));
    }

    @Test
    void testToolUsageExample() {
        runSample(() -> ToolUsageExample.main(new String[]{}));
    }

    @Test
    void testMultiTurnExample() {
        runSample(() -> MultiTurnExample.main(new String[]{}));
    }

    @Test
    void testMemoryExample() {
        runSample(() -> MemoryExample.main(new String[]{}));
    }

    @Test
    void testMiddlewareExample() {
        runSample(() -> MiddlewareExample.main(new String[]{}));
    }

    @Test
    void testWorkflowExample() {
        runSample(() -> WorkflowExample.main(new String[]{}));
    }

    @Test
    void testComplexWorkflowExample() {
        runSample(() -> ComplexWorkflowExample.main(new String[]{}));
    }

    @Test
    void testMultiAgentExample() {
        runSample(() -> MultiAgentExample.main(new String[]{}));
    }

    @Test
    void testEndToEndExample() {
        runSample(() -> EndToEndExample.main(new String[]{}));
    }
    
    @Test
    void testAutoGenMigrationExample() {
        runSample(() -> AutoGenMigrationExample.main(new String[]{}));
    }

    @Test
    void testGroupChatMigrationExample() {
        runSample(() -> GroupChatMigrationExample.main(new String[]{}));
    }
    
    // HostingExample starts a server, so we skip it or run it in a separate thread and kill it?
    // ObservabilityExample is safe to run.
    @Test
    void testObservabilityExample() {
        runSample(() -> ObservabilityExample.main(new String[]{}));
    }
}

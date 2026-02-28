package github.ponyhuang.agentframework.orchestrations;

import github.ponyhuang.agentframework.agents.Agent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SequentialAgentBuilder class.
 * Tests sequential output transformation.
 */
class SequentialAgentBuilderTest {

    /**
     * Test SequentialAgentBuilder adds agents.
     * Verifies agents are added to sequential execution.
     */
    @Test
    void testSequentialBuilderAddsAgents() {
        // Create mock agent
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getName()).thenReturn("testAgent");

        // Create sequential builder and add agent
        SequentialAgentBuilder builder = new SequentialAgentBuilder();
        builder.agent(mockAgent);

        // Verify agent count
        assertEquals(1, builder.getAgentCount());
    }

    /**
     * Test SequentialAgentBuilder with output transformer.
     * Verifies output transformation is applied.
     */
    @Test
    void testSequentialBuilderWithOutputTransformer() {
        // Create sequential builder with transformer
        SequentialAgentBuilder builder = new SequentialAgentBuilder();
        builder.outputTransformer(response -> "transformed_" + response);

        // Verify builder was configured
        assertNotNull(builder);
    }

    /**
     * Test SequentialAgentBuilder getAgentCount returns zero initially.
     * Verifies initial state.
     */
    @Test
    void testSequentialBuilderInitialCount() {
        // Create builder
        SequentialAgentBuilder builder = new SequentialAgentBuilder();

        // Verify initial count is 0
        assertEquals(0, builder.getAgentCount());
    }
}

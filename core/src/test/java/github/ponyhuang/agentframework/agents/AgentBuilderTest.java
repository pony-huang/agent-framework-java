package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.providers.ChatClient;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AgentBuilder new methods.
 */
class AgentBuilderTest {

    @Test
    void testDisallowedTools() {
        AgentBuilder builder = AgentBuilder.builder()
                .disallowedTools(Set.of("Bash", "Write"));

        assertTrue(builder.getDisallowedTools().contains("Bash"));
        assertTrue(builder.getDisallowedTools().contains("Write"));
        assertEquals(2, builder.getDisallowedTools().size());
    }

    @Test
    void testMaxBudgetUsd() {
        AgentBuilder builder = AgentBuilder.builder()
                .maxBudgetUsd(1.50);

        assertEquals(1.50, builder.getMaxBudgetUsd(), 0.001);
    }

    @Test
    void testMaxBudgetUsdZeroMeansUnlimited() {
        AgentBuilder builder = AgentBuilder.builder()
                .maxBudgetUsd(0);

        assertEquals(0.0, builder.getMaxBudgetUsd(), 0.001);
    }

    @Test
    void testMaxBudgetUsdNegativeMeansUnlimited() {
        AgentBuilder builder = AgentBuilder.builder()
                .maxBudgetUsd(-5.0);

        assertEquals(0.0, builder.getMaxBudgetUsd(), 0.001);
    }

    @Test
    void testFallbackModel() {
        AgentBuilder builder = AgentBuilder.builder()
                .fallbackModel("claude-haiku-3-5");

        assertEquals("claude-haiku-3-5", builder.getFallbackModel());
    }

    @Test
    void testPermissionMode() {
        AgentBuilder builder = AgentBuilder.builder()
                .permissionMode(PermissionMode.PLAN);

        assertEquals(PermissionMode.PLAN, builder.getPermissionMode());
    }

    @Test
    void testDefaultPermissionMode() {
        AgentBuilder builder = AgentBuilder.builder();

        assertEquals(PermissionMode.DEFAULT, builder.getPermissionMode());
    }

    @Test
    void testPermissionModeNull() {
        AgentBuilder builder = AgentBuilder.builder()
                .permissionMode(null);

        // Should default to DEFAULT
        assertEquals(PermissionMode.DEFAULT, builder.getPermissionMode());
    }

    @Test
    void testAllowedTools() {
        AgentBuilder builder = AgentBuilder.builder()
                .allowedTools(Set.of("Read", "Glob", "Grep"));

        assertTrue(builder.getAllowedTools().contains("Read"));
        assertTrue(builder.getAllowedTools().contains("Glob"));
        assertTrue(builder.getAllowedTools().contains("Grep"));
    }

    @Test
    void testNullAllowedTools() {
        AgentBuilder builder = AgentBuilder.builder()
                .allowedTools(null);

        assertNotNull(builder.getAllowedTools());
    }

    @Test
    void testNullDisallowedTools() {
        AgentBuilder builder = AgentBuilder.builder()
                .disallowedTools(null);

        assertNotNull(builder.getDisallowedTools());
    }

    @Test
    void testAgentDefinition() {
        ChatClient mockClient = mock(ChatClient.class);
        when(mockClient.getModel()).thenReturn("test-model");

        AgentDefinition agentDef = AgentDefinition.builder()
                .name("test-agent")
                .prompt("You are a test agent.")
                .build();

        AgentBuilder builder = AgentBuilder.builder()
                .client(mockClient)
                .agent(agentDef);

        assertTrue(builder.getAgents().containsKey("test-agent"));
        assertEquals(agentDef, builder.getAgents().get("test-agent"));
    }

    @Test
    void testMultipleAgents() {
        ChatClient mockClient = mock(ChatClient.class);
        when(mockClient.getModel()).thenReturn("test-model");

        AgentDefinition agent1 = AgentDefinition.builder()
                .name("agent1")
                .prompt("Agent 1")
                .build();

        AgentDefinition agent2 = AgentDefinition.builder()
                .name("agent2")
                .prompt("Agent 2")
                .build();

        AgentBuilder builder = AgentBuilder.builder()
                .client(mockClient)
                .agents(java.util.Map.of("agent1", agent1, "agent2", agent2));

        assertEquals(2, builder.getAgents().size());
    }
}

package github.ponyhuang.agentframework.agents;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AgentDefinition.
 */
class AgentDefinitionTest {

    @Test
    void testBuilder() {
        AgentDefinition agent = AgentDefinition.builder()
                .name("test-agent")
                .description("A test agent")
                .prompt("You are a test agent.")
                .tools(java.util.Set.of("Read", "Write"))
                .model("claude-sonnet-4-5")
                .build();

        assertEquals("test-agent", agent.getName());
        assertEquals("A test agent", agent.getDescription());
        assertEquals("You are a test agent.", agent.getPrompt());
        assertEquals(2, agent.getTools().size());
        assertTrue(agent.getTools().contains("Read"));
        assertEquals("claude-sonnet-4-5", agent.getModel());
    }

    @Test
    void testBuilderWithNullTools() {
        AgentDefinition agent = AgentDefinition.builder()
                .name("test-agent")
                .prompt("You are a test agent.")
                .build();

        assertNotNull(agent.getTools());
        assertTrue(agent.getTools().isEmpty());
        assertNull(agent.getModel());
    }

    @Test
    void testConstructor() {
        AgentDefinition agent = new AgentDefinition(
                "my-agent",
                "Description",
                "System prompt",
                java.util.Set.of("Read"),
                "claude-haiku-3-5"
        );

        assertEquals("my-agent", agent.getName());
        assertEquals("Description", agent.getDescription());
        assertEquals("System prompt", agent.getPrompt());
        assertEquals(1, agent.getTools().size());
        assertEquals("claude-haiku-3-5", agent.getModel());
    }

    @Test
    void testNameRequired() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentDefinition.builder()
                        .prompt("test")
                        .build()
        );
    }

    @Test
    void testPromptRequired() {
        assertThrows(IllegalArgumentException.class, () ->
                AgentDefinition.builder()
                        .name("test")
                        .build()
        );
    }
}

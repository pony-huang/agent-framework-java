package github.ponyhuang.agentframework.types;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatCompleteParams class.
 * Tests params building, getEffectiveSystem(), and various parameters.
 */
class ChatCompleteParamsTest {

    /**
     * Test ChatCompleteParams.builder() creates params with all fields.
     * Verifies builder pattern works correctly.
     */
    @Test
    void testChatCompleteParamsBuilder() {
        List<Message> messages = List.of(Message.user("Hello"));
        List<Map<String, Object>> tools = List.of(Map.of("name", "test_tool"));

        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(messages)
                .model("gpt-4")
                .temperature(0.7)
                .maxTokens(1000)
                .topP(0.9)
                .tools(tools)
                .system("You are a helpful assistant")
                .stop("\n")
                .n(1)
                .presencePenalty(0.0)
                .frequencyPenalty(0.0)
                .build();

        // Verify all fields
        assertEquals("gpt-4", params.getModel());
        assertEquals(0.7, params.getTemperature());
        assertEquals(1000, params.getMaxTokens());
        assertEquals(0.9, params.getTopP());
        assertEquals(1, params.getTools().size());
        assertEquals("You are a helpful assistant", params.getSystem());
        assertEquals("\n", params.getStop());
        assertEquals(1, params.getN());
    }

    /**
     * Test getEffectiveSystem() returns system field when present.
     * Verifies system field takes precedence.
     */
    @Test
    void testGetEffectiveSystemWithSystemField() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(Message.user("Hello")))
                .system("System prompt from field")
                .build();

        // Verify system field is returned
        assertEquals("System prompt from field", params.getEffectiveSystem());
    }

    /**
     * Test getEffectiveSystem() returns system from messages list when no system field.
     * Verifies fallback to messages list.
     */
    @Test
    void testGetEffectiveSystemFromMessagesList() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(
                        Message.system("System from messages"),
                        Message.user("Hello")
                ))
                .build();

        // Verify system from messages is returned
        assertEquals("System from messages", params.getEffectiveSystem());
    }

    /**
     * Test getEffectiveSystem() returns system field over messages list.
     * Verifies field takes precedence over messages.
     */
    @Test
    void testGetEffectiveSystemPrefersFieldOverMessages() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(
                        Message.system("System from messages"),
                        Message.user("Hello")
                ))
                .system("System from field")
                .build();

        // Verify field takes precedence
        assertEquals("System from field", params.getEffectiveSystem());
    }

    /**
     * Test getEffectiveSystem() returns null when no system.
     * Verifies null handling.
     */
    @Test
    void testGetEffectiveSystemReturnsNull() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(Message.user("Hello")))
                .build();

        // Verify null is returned
        assertNull(params.getEffectiveSystem());
    }

    /**
     * Test getEffectiveSystem() with empty messages.
     * Verifies null handling for empty list.
     */
    @Test
    void testGetEffectiveSystemWithEmptyMessages() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of())
                .build();

        // Verify null is returned
        assertNull(params.getEffectiveSystem());
    }

    /**
     * Test getEffectiveSystem() with no system message in list.
     * Verifies returns null when no system message.
     */
    @Test
    void testGetEffectiveSystemNoSystemMessage() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(Message.user("Hello")))
                .build();

        // Verify null is returned
        assertNull(params.getEffectiveSystem());
    }

    /**
     * Test ChatCompleteParams with tools.
     * Verifies tools are stored correctly.
     */
    @Test
    void testToolsParameter() {
        List<Map<String, Object>> tools = List.of(
                Map.of("name", "tool1", "description", "First tool"),
                Map.of("name", "tool2", "description", "Second tool")
        );

        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(Message.user("Hello")))
                .tools(tools)
                .build();

        // Verify tools are stored
        assertEquals(2, params.getTools().size());
    }

    /**
     * Test ChatCompleteParams with null messages.
     * Verifies null handling.
     */
    @Test
    void testNullMessages() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .build();

        // Verify null messages returns null
        assertNull(params.getMessages());
        assertNull(params.getEffectiveSystem());
    }

    /**
     * Test ChatCompleteParams with penalty parameters.
     * Verifies presence and frequency penalty.
     */
    @Test
    void testPenaltyParameters() {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(Message.user("Hello")))
                .presencePenalty(0.5)
                .frequencyPenalty(0.3)
                .build();

        // Verify penalty values
        assertEquals(0.5, params.getPresencePenalty());
        assertEquals(0.3, params.getFrequencyPenalty());
    }

    /**
     * Test ChatCompleteParams with extra properties.
     * Verifies extra properties are stored.
     */
    @Test
    void testExtraProperties() {
        Map<String, Object> extra = Map.of("custom_key", "custom_value");

        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(List.of(Message.user("Hello")))
                .extraProperties(extra)
                .build();

        // Verify extra properties
        assertEquals("custom_value", params.getExtraProperties().get("custom_key"));
    }
}

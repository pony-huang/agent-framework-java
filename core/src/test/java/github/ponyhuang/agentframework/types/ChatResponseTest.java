package github.ponyhuang.agentframework.types;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChatResponse class.
 * Tests ChatResponse builder, choices, usage, and helper methods.
 */
class ChatResponseTest {

    /**
     * Test ChatResponse.builder() creates a response with all fields.
     * Verifies builder pattern works correctly.
     */
    @Test
    void testChatResponseBuilder() {
        Message assistantMessage = Message.assistant("Hello!");
        ChatResponse.Choice choice = new ChatResponse.Choice(0, assistantMessage, "stop");
        ChatResponse.Usage usage = new ChatResponse.Usage(100, 50, 150);

        ChatResponse response = ChatResponse.builder()
                .id("chatcmpl-123")
                .created(1234567890L)
                .model("gpt-4")
                .choices(List.of(choice))
                .usage(usage)
                .finishReason("stop")
                .build();

        // Verify all fields
        assertEquals("chatcmpl-123", response.getId());
        assertEquals(1234567890L, response.getCreated());
        assertEquals("gpt-4", response.getModel());
        assertEquals(1, response.getChoices().size());
        assertEquals(150, response.getUsage().getTotalTokens());
        assertEquals("stop", response.getFinishReason());
    }

    /**
     * Test ChatResponse.getMessage() returns first choice's message.
     * Verifies getMessage() helper method.
     */
    @Test
    void testGetMessage() {
        Message assistantMessage = Message.assistant("Response text");
        ChatResponse.Choice choice = new ChatResponse.Choice(0, assistantMessage, "stop");

        ChatResponse response = ChatResponse.builder()
                .choices(List.of(choice))
                .build();

        // Verify getMessage returns the first choice's message
        assertNotNull(response.getMessage());
        assertEquals("Response text", response.getMessage().getText());
    }

    /**
     * Test ChatResponse.getMessage() returns null when no choices.
     * Verifies null handling for empty choices.
     */
    @Test
    void testGetMessageReturnsNullForEmptyChoices() {
        ChatResponse response = ChatResponse.builder()
                .choices(List.of())
                .build();

        // Verify null is returned for empty choices
        assertNull(response.getMessage());
    }

    /**
     * Test ChatResponse.getMessage() returns null when choices is null.
     * Verifies null handling.
     */
    @Test
    void testGetMessageReturnsNullForNullChoices() {
        ChatResponse response = ChatResponse.builder()
                .build();

        // Verify null is returned for null choices
        assertNull(response.getMessage());
    }

    /**
     * Test ChatResponse.hasFunctionCall() returns true when function call exists.
     * Verifies function call detection.
     */
    @Test
    void testHasFunctionCallReturnsTrue() {
        Map<String, Object> functionCall = Map.of(
                "name", "test_function",
                "arguments", Map.of()
        );
        Message message = Message.assistantFunctionCall(functionCall);
        ChatResponse.Choice choice = new ChatResponse.Choice(0, message, "tool_calls");

        ChatResponse response = ChatResponse.builder()
                .choices(List.of(choice))
                .build();

        // Verify hasFunctionCall returns true
        assertTrue(response.hasFunctionCall());
    }

    /**
     * Test ChatResponse.hasFunctionCall() returns false when no function call.
     * Verifies proper false return.
     */
    @Test
    void testHasFunctionCallReturnsFalse() {
        Message message = Message.assistant("Regular response");
        ChatResponse.Choice choice = new ChatResponse.Choice(0, message, "stop");

        ChatResponse response = ChatResponse.builder()
                .choices(List.of(choice))
                .build();

        // Verify hasFunctionCall returns false
        assertFalse(response.hasFunctionCall());
    }

    /**
     * Test ChatResponse.hasFunctionCall() returns false when choices is null.
     * Verifies null handling.
     */
    @Test
    void testHasFunctionCallReturnsFalseForNullChoices() {
        ChatResponse response = ChatResponse.builder()
                .build();

        // Verify hasFunctionCall returns false for null choices
        assertFalse(response.hasFunctionCall());
    }

    /**
     * Test ChatResponse.Choice class.
     * Verifies Choice stores index, message, and finishReason.
     */
    @Test
    void testChoiceClass() {
        Message message = Message.assistant("Test response");
        ChatResponse.Choice choice = new ChatResponse.Choice(2, message, "length");

        // Verify all fields
        assertEquals(2, choice.getIndex());
        assertEquals("Test response", choice.getMessage().getText());
        assertEquals("length", choice.getFinishReason());
    }

    /**
     * Test ChatResponse.Usage class.
     * Verifies Usage stores token counts.
     */
    @Test
    void testUsageClass() {
        ChatResponse.Usage usage = new ChatResponse.Usage(100, 50, 150);

        // Verify all token counts
        assertEquals(100, usage.getPromptTokens());
        assertEquals(50, usage.getCompletionTokens());
        assertEquals(150, usage.getTotalTokens());
    }

    /**
     * Test ChatResponse with multiple choices.
     * Verifies multiple choices are stored correctly.
     */
    @Test
    void testMultipleChoices() {
        Message message1 = Message.assistant("Response 1");
        Message message2 = Message.assistant("Response 2");

        ChatResponse response = ChatResponse.builder()
                .choices(List.of(
                        new ChatResponse.Choice(0, message1, "stop"),
                        new ChatResponse.Choice(1, message2, "stop")
                ))
                .build();

        // Verify both choices are present
        assertEquals(2, response.getChoices().size());
        assertEquals("Response 1", response.getChoices().get(0).getMessage().getText());
        assertEquals("Response 2", response.getChoices().get(1).getMessage().getText());
    }

    /**
     * Test ChatResponse with extra properties.
     * Verifies extra properties are stored.
     */
    @Test
    void testExtraProperties() {
        Map<String, Object> extraProps = Map.of("key1", "value1", "key2", 42);

        ChatResponse response = ChatResponse.builder()
                .extraProperties(extraProps)
                .build();

        // Verify extra properties
        assertEquals("value1", response.getExtraProperties().get("key1"));
        assertEquals(42, response.getExtraProperties().get("key2"));
    }

    /**
     * Test ChatResponse.getFinishReason() returns from first choice.
     * Verifies convenience method.
     */
    @Test
    void testFinishReasonFromBuilder() {
        Message message = Message.assistant("Response");
        ChatResponse.Choice choice = new ChatResponse.Choice(0, message, "stop");

        ChatResponse response = ChatResponse.builder()
                .choices(List.of(choice))
                .finishReason("stop")
                .build();

        // Verify finish reason from builder
        assertEquals("stop", response.getFinishReason());
    }
}

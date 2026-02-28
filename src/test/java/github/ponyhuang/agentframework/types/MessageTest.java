package github.ponyhuang.agentframework.types;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Message class.
 * Tests Message creation using builder pattern and factory methods.
 */
class MessageTest {

    /**
     * Test Message creation using builder pattern.
     * Verifies that the builder correctly sets role, contents, name, and toolCallId.
     */
    @Test
    void testMessageBuilder() {
        // Create a message using the builder pattern
        Message message = Message.builder()
                .role(Role.USER)
                .addContent(Content.text("Hello, world!"))
                .name("testUser")
                .build();

        // Verify the message has the correct role
        assertEquals(Role.USER, message.getRole());
        // Verify the message has the correct text content
        assertEquals("Hello, world!", message.getText());
        // Verify the message has the correct name
        assertEquals("testUser", message.getName());
    }

    /**
     * Test Message.user() factory method.
     * Verifies it creates a message with USER role.
     */
    @Test
    void testUserFactoryMethod() {
        Message message = Message.user("Test message");

        // Verify role is USER
        assertEquals(Role.USER, message.getRole());
        // Verify text content
        assertEquals("Test message", message.getText());
    }

    /**
     * Test Message.assistant() factory method.
     * Verifies it creates a message with ASSISTANT role.
     */
    @Test
    void testAssistantFactoryMethod() {
        Message message = Assistant("Response from assistant");

        // Verify role is ASSISTANT
        assertEquals(Role.ASSISTANT, message.getRole());
        // Verify text content
        assertEquals("Response from assistant", message.getText());
    }

    /**
     * Test Message.system() factory method.
     * Verifies it creates a message with SYSTEM role.
     */
    @Test
    void testSystemFactoryMethod() {
        Message message = Message.system("You are a helpful assistant");

        // Verify role is SYSTEM
        assertEquals(Role.SYSTEM, message.getRole());
        // Verify text content
        assertEquals("You are a helpful assistant", message.getText());
    }

    /**
     * Test Message.tool() factory method.
     * Verifies it creates a message with TOOL role and function result.
     */
    @Test
    void testToolFactoryMethod() {
        Message message = Message.tool("call_123", "get_weather", "Sunny");

        // Verify role is TOOL
        assertEquals(Role.TOOL, message.getRole());
        // Verify toolCallId
        assertEquals("call_123", message.getToolCallId());
    }

    /**
     * Test Message.assistantFunctionCall() factory method.
     * Verifies it creates a message with function call content.
     */
    @Test
    void testAssistantFunctionCallFactoryMethod() {
        Map<String, Object> functionCall = Map.of(
                "id", "call_123",
                "name", "get_weather",
                "arguments", Map.of("city", "Beijing")
        );

        Message message = Message.assistantFunctionCall(functionCall);

        // Verify role is ASSISTANT
        assertEquals(Role.ASSISTANT, message.getRole());
        // Verify function call is present
        assertNotNull(message.getFunctionCall());
        assertEquals("get_weather", message.getFunctionCall().get("name"));
    }

    /**
     * Test Message.getText() with empty contents.
     * Verifies it returns empty string when no content.
     */
    @Test
    void testGetTextWithEmptyContents() {
        Message message = Message.builder()
                .role(Role.USER)
                .build();

        // Verify empty string is returned
        assertEquals("", message.getText());
    }

    /**
     * Test Message.getText() with multiple content parts.
     * Verifies it returns the first TEXT content.
     */
    @Test
    void testGetTextWithMultipleContents() {
        Message message = Message.builder()
                .role(Role.USER)
                .addContent(Content.text("First"))
                .addContent(Content.text("Second"))
                .build();

        // Verify first text content is returned
        assertEquals("First", message.getText());
    }

    /**
     * Test Message.getFunctionCall() returns null when no function call.
     * Verifies proper null handling.
     */
    @Test
    void testGetFunctionCallReturnsNull() {
        Message message = Message.user("Simple message");

        // Verify null is returned when no function call
        assertNull(message.getFunctionCall());
    }

    /**
     * Test Message with multiple contents including function call.
     * Verifies getFunctionCall correctly extracts function call content.
     */
    @Test
    void testGetFunctionCallWithMultipleContents() {
        Map<String, Object> functionCall = Map.of(
                "name", "test_function",
                "arguments", Map.of()
        );

        Message message = Message.builder()
                .role(Role.ASSISTANT)
                .addContent(Content.text("Thinking..."))
                .addContent(Content.fromFunctionCall(functionCall))
                .build();

        // Verify function call is extracted correctly
        assertNotNull(message.getFunctionCall());
        assertEquals("test_function", message.getFunctionCall().get("name"));
    }

    /**
     * Test Message.user() with List<Content>.
     * Verifies factory method accepts content list.
     */
    @Test
    void testUserWithContentList() {
        List<Content> contents = List.of(
                Content.text("Part 1"),
                Content.text("Part 2")
        );

        Message message = Message.user(contents);

        // Verify role and contents
        assertEquals(Role.USER, message.getRole());
        assertEquals(2, message.getContents().size());
    }

    /**
     * Helper method to create assistant message (name conflict with local variable).
     */
    private Message Assistant(String content) {
        return Message.assistant(content);
    }
}

package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.TextBlock;
import github.ponyhuang.agentframework.types.block.ThinkingBlock;
import github.ponyhuang.agentframework.types.block.ToolResultBlock;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void testUserMessageCreate() {
        UserMessage message = UserMessage.create("Hello, world!");

        assertEquals("user", message.getRole());
        assertEquals("Hello, world!", message.getTextContent());
        assertFalse(message.getBlocks().isEmpty());
        assertTrue(message.getBlocks().get(0) instanceof TextBlock);
    }

    @Test
    void testUserMessageWithName() {
        UserMessage message = UserMessage.create("Hello", "John");

        assertEquals("user", message.getRole());
        assertEquals("John", message.getName());
        assertEquals("Hello", message.getTextContent());
    }

    @Test
    void testUserMessageWithBlocks() {
        List<github.ponyhuang.agentframework.types.block.Block> blocks = List.of(
            new TextBlock("Part 1"),
            new TextBlock("Part 2")
        );
        UserMessage message = UserMessage.fromBlocks(blocks);

        assertEquals("user", message.getRole());
        assertEquals(2, message.getBlocks().size());
        assertEquals("Part 1", message.getTextContent());
    }

    @Test
    void testUserMessageEmpty() {
        UserMessage message = UserMessage.create();

        assertEquals("user", message.getRole());
        assertEquals("", message.getTextContent());
        assertTrue(message.getBlocks().isEmpty());
    }

    @Test
    void testSystemMessageCreate() {
        SystemMessage message = SystemMessage.create("You are a helpful assistant.");

        assertEquals("system", message.getRole());
        assertEquals("You are a helpful assistant.", message.getTextContent());
    }

    @Test
    void testSystemMessageWithBlocks() {
        List<github.ponyhuang.agentframework.types.block.Block> blocks = List.of(
            new ThinkingBlock("System thinking...")
        );
        SystemMessage message = SystemMessage.fromBlocks(blocks);

        assertEquals("system", message.getRole());
        assertTrue(message.getBlocks().get(0) instanceof ThinkingBlock);
    }

    @Test
    void testSystemMessageEmpty() {
        SystemMessage message = SystemMessage.create();

        assertEquals("system", message.getRole());
        assertEquals("", message.getTextContent());
    }

    @Test
    void testAssistantMessageCreate() {
        AssistantMessage message = AssistantMessage.create("This is a response.");

        assertEquals("assistant", message.getRole());
        assertEquals("This is a response.", message.getTextContent());
        assertFalse(message.hasFunctionCall());
    }

    @Test
    void testAssistantMessageWithBlocks() {
        List<github.ponyhuang.agentframework.types.block.Block> blocks = List.of(
            new TextBlock("Hello"),
            new ThinkingBlock("Thinking about the answer..."),
            new TextBlock("Here is my answer.")
        );
        AssistantMessage message = AssistantMessage.fromBlocks(blocks);

        assertEquals("assistant", message.getRole());
        assertEquals(3, message.getBlocks().size());
    }

    @Test
    void testAssistantMessageWithFunctionCall() {
        Map<String, Object> args = Map.of("location", "Paris", "units", "celsius");
        AssistantMessage message = AssistantMessage.createWithFunctionCall("get_weather", args);

        assertEquals("assistant", message.getRole());
        assertTrue(message.hasFunctionCall());
        assertEquals("get_weather", message.getFunctionName());
        assertEquals("Paris", message.getFunctionArguments().get("location"));
        assertEquals("celsius", message.getFunctionArguments().get("units"));
    }

    @Test
    void testAssistantMessageWithFunctionCallAndId() {
        Map<String, Object> args = Map.of("param", "value");
        AssistantMessage message = AssistantMessage.createWithFunctionCall("call_123", "my_function", args);

        assertTrue(message.hasFunctionCall());
        assertEquals("call_123", message.getFunctionCallId());
        assertEquals("my_function", message.getFunctionName());
    }

    @Test
    void testAssistantMessageFunctionCallGetters() {
        Map<String, Object> functionCall = Map.of(
            "id", "call_abc",
            "name", "test_function",
            "arguments", Map.of("key", "value")
        );
        AssistantMessage message = AssistantMessage.create().withFunctionCall(functionCall);

        assertEquals("call_abc", message.getFunctionCallId());
        assertEquals("test_function", message.getFunctionName());
        assertEquals("value", ((Map<String, Object>) message.getFunctionArguments()).get("key"));
    }

    @Test
    void testResultMessageCreate() {
        ResultMessage message = ResultMessage.create("call_123", "Weather: Sunny, 25C");

        assertEquals("tool", message.getRole());
        assertEquals("call_123", message.getToolCallId());
        assertEquals("Weather: Sunny, 25C", message.getResultContent());
        assertFalse(message.isError());
    }

    @Test
    void testResultMessageWithToolName() {
        ResultMessage message = ResultMessage.create("call_123", "Sunny");

        assertEquals("tool", message.getRole());
        assertEquals("call_123", message.getToolCallId());
        assertEquals("Sunny", message.getResultContent());
    }

    @Test
    void testResultMessageWithError() {
        ResultMessage message = ResultMessage.create("call_error", "Error: Something went wrong", true);

        assertEquals("tool", message.getRole());
        assertEquals("Error: Something went wrong", message.getResultContent());
        assertTrue(message.isError());
    }

    @Test
    void testResultMessageFromFunctionResult() {
        ResultMessage message = ResultMessage.fromFunctionResult("call_xyz", "my_tool", "Success!");

        assertEquals("tool", message.getRole());
        assertEquals("call_xyz", message.getToolCallId());
        assertEquals("Success!", message.getResultContent());
    }

    @Test
    void testAbstractMessageHasToolUse() {
        AssistantMessage messageWithTool = AssistantMessage.createWithFunctionCall("fn", Map.of());
        assertTrue(messageWithTool.hasToolUse());

        AssistantMessage messageWithoutTool = AssistantMessage.create("Just text");
        assertFalse(messageWithoutTool.hasToolUse());
    }

    @Test
    void testAbstractMessageHasToolResult() {
        ResultMessage messageWithResult = ResultMessage.create("call_1", "result");
        assertTrue(messageWithResult.hasToolResult());

        UserMessage userMessage = UserMessage.create("hello");
        assertFalse(userMessage.hasToolResult());
    }

    @Test
    void testMessageImplementsInterface() {
        UserMessage userMessage = UserMessage.create("test");
        assertTrue(userMessage instanceof Message);

        SystemMessage systemMessage = SystemMessage.create("test");
        assertTrue(systemMessage instanceof Message);

        AssistantMessage assistantMessage = AssistantMessage.create("test");
        assertTrue(assistantMessage instanceof Message);

        ResultMessage resultMessage = ResultMessage.create("call_1", "result");
        assertTrue(resultMessage instanceof Message);
    }

    @Test
    void testUserMessageWithText() {
        UserMessage original = UserMessage.create("Original");
        UserMessage updated = original.withText("Updated text");

        assertEquals("user", updated.getRole());
        assertEquals("Updated text", updated.getTextContent());
    }

    @Test
    void testAssistantMessageWithText() {
        AssistantMessage original = AssistantMessage.create("Original");
        AssistantMessage updated = original.withText("Updated text");

        assertEquals("assistant", updated.getRole());
        assertEquals("Updated text", updated.getTextContent());
    }

    @Test
    void testResultMessageWithToolCallId() {
        ResultMessage original = ResultMessage.create("call_1", "result");
        ResultMessage updated = original.withToolCallId("call_2");

        assertEquals("call_2", updated.getToolCallId());
        assertEquals("result", updated.getResultContent());
    }
}

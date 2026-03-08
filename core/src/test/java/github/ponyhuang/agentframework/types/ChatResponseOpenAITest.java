package github.ponyhuang.agentframework.types;

import github.ponyhuang.agentframework.types.block.TextBlock;
import github.ponyhuang.agentframework.types.block.ToolResultBlock;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.ResultMessage;
import github.ponyhuang.agentframework.types.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatResponseOpenAITest {

    @Test
    void testChatResponseBuilderWithMessages() {
        UserMessage userMsg = UserMessage.create("Hello");
        AssistantMessage assistantMsg = AssistantMessage.create("Hi there!");

        ChatResponse response = ChatResponse.builder()
                .id("chatcmpl-123")
                .created(1234567890L)
                .model("gpt-4")
                .messages(List.of(userMsg, assistantMsg))
                .usage(new ChatResponse.Usage(10, 5, 15))
                .finishReason("stop")
                .build();

        assertEquals("chatcmpl-123", response.getId());
        assertEquals(1234567890L, response.getCreated());
        assertEquals("gpt-4", response.getModel());
        assertEquals(2, response.getMessages().size());
        assertEquals(15, response.getUsage().getTotalTokens());
        assertEquals("stop", response.getFinishReason());
    }

    @Test
    void testGetFirstMessage() {
        UserMessage userMsg = UserMessage.create("Hello");
        AssistantMessage assistantMsg = AssistantMessage.create("Hi there!");

        ChatResponse response = ChatResponse.builder()
                .messages(List.of(userMsg, assistantMsg))
                .build();

        assertNotNull(response.getFirstMessage());
        assertEquals("user", response.getFirstMessage().getRole());
    }

    @Test
    void testGetAssistantMessage() {
        UserMessage userMsg = UserMessage.create("Hello");
        AssistantMessage assistantMsg = AssistantMessage.create("Hi there!");

        ChatResponse response = ChatResponse.builder()
                .messages(List.of(userMsg, assistantMsg))
                .build();

        AssistantMessage result = response.getAssistantMessage();
        assertNotNull(result);
        assertEquals("Hi there!", result.getTextContent());
    }

    @Test
    void testHasFunctionCall() {
        UserMessage userMsg = UserMessage.create("What's the weather?");
        AssistantMessage assistantMsg = AssistantMessage.createWithFunctionCall("get_weather",
                Map.of("location", "Paris"));

        ChatResponse responseWithFunction = ChatResponse.builder()
                .messages(List.of(userMsg, assistantMsg))
                .build();
        assertTrue(responseWithFunction.hasFunctionCall());

        AssistantMessage regularMsg = AssistantMessage.create("Just text");
        ChatResponse responseWithoutFunction = ChatResponse.builder()
                .messages(List.of(regularMsg))
                .build();
        assertFalse(responseWithoutFunction.hasFunctionCall());
    }

    @Test
    void testGetToolCalls() {
        AssistantMessage assistantMsg = AssistantMessage.createWithFunctionCall("call_1", "get_weather",
                Map.of("location", "Paris"));

        ChatResponse response = ChatResponse.builder()
                .messages(List.of(assistantMsg))
                .build();

        List<ToolUseBlock> toolCalls = response.getToolCalls();
        assertEquals(1, toolCalls.size());
        assertEquals("get_weather", toolCalls.get(0).getName());
        assertEquals("call_1", toolCalls.get(0).getId());
    }

    @Test
    void testToOpenAIMessage() {
        AssistantMessage assistantMsg = AssistantMessage.create("Hello, world!");
        ChatResponse response = ChatResponse.builder()
                .id("chatcmpl-123")
                .created(1234567890L)
                .model("gpt-4")
                .messages(List.of(assistantMsg))
                .usage(new ChatResponse.Usage(10, 5, 15))
                .finishReason("stop")
                .build();

        Map<String, Object> openAI = response.toOpenAIMessage();

        assertEquals("chatcmpl-123", openAI.get("id"));
        assertEquals("gpt-4", openAI.get("model"));
        assertNotNull(openAI.get("choices"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) openAI.get("choices");
        assertEquals(1, choices.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        assertEquals("assistant", message.get("role"));
        assertEquals("Hello, world!", message.get("content"));
    }

    @Test
    void testToOpenAIMessageWithFunctionCall() {
        AssistantMessage assistantMsg = AssistantMessage.createWithFunctionCall("get_weather",
                Map.of("location", "Paris"));

        ChatResponse response = ChatResponse.builder()
                .messages(List.of(assistantMsg))
                .finishReason("tool_calls")
                .build();

        Map<String, Object> openAI = response.toOpenAIMessage();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) openAI.get("choices");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        assertNotNull(message.get("function_call"));
        @SuppressWarnings("unchecked")
        Map<String, Object> functionCall = (Map<String, Object>) message.get("function_call");
        assertEquals("get_weather", functionCall.get("name"));
    }

    @Test
    void testToOpenAIMessageWithToolResult() {
        ResultMessage resultMsg = ResultMessage.create("call_123", "Sunny, 25C");

        ChatResponse response = ChatResponse.builder()
                .messages(List.of(resultMsg))
                .build();

        Map<String, Object> openAI = response.toOpenAIMessage();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) openAI.get("choices");
        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

        assertEquals("tool", message.get("role"));
        assertEquals("call_123", message.get("tool_call_id"));
        assertEquals("Sunny, 25C", message.get("content"));
    }

    @Test
    void testFromOpenAIFormat() {
        Map<String, Object> openAIFormat = Map.of(
                "id", "chatcmpl-abc",
                "created", 1234567890L,
                "model", "gpt-4",
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of(
                                "role", "assistant",
                                "content", "Hello from OpenAI!"
                        ),
                        "finish_reason", "stop"
                )),
                "usage", Map.of(
                        "prompt_tokens", 10,
                        "completion_tokens", 5,
                        "total_tokens", 15
                )
        );

        ChatResponse response = ChatResponse.fromOpenAIFormat(openAIFormat);

        assertEquals("chatcmpl-abc", response.getId());
        assertEquals("gpt-4", response.getModel());
        assertEquals(1, response.getMessages().size());
        assertEquals("assistant", response.getFirstMessage().getRoleAsString());
        assertEquals("Hello from OpenAI!", response.getFirstMessage().getTextContent());
        assertEquals(15, response.getUsage().getTotalTokens());
        assertEquals("stop", response.getFinishReason());
    }

    @Test
    void testFromOpenAIFormatWithFunctionCall() {
        Map<String, Object> openAIFormat = Map.of(
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of(
                                "role", "assistant",
                                "function_call", Map.of(
                                        "name", "get_weather",
                                        "arguments", Map.of("location", "Paris")
                                )
                        ),
                        "finish_reason", "tool_calls"
                ))
        );

        ChatResponse response = ChatResponse.fromOpenAIFormat(openAIFormat);

        assertTrue(response.hasFunctionCall());
        AssistantMessage assistantMsg = response.getAssistantMessage();
        assertNotNull(assistantMsg);
        assertEquals("get_weather", assistantMsg.getFunctionName());
    }

    @Test
    void testFromOpenAIFormatWithToolResult() {
        Map<String, Object> openAIFormat = Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of(
                                "role", "tool",
                                "tool_call_id", "call_xyz",
                                "content", "The result is 42"
                        )
                ))
        );

        ChatResponse response = ChatResponse.fromOpenAIFormat(openAIFormat);

        assertEquals(1, response.getMessages().size());
        assertEquals("tool", response.getFirstMessage().getRole());
        assertEquals("call_xyz", ((ResultMessage) response.getFirstMessage()).getToolCallId());
        assertEquals("The result is 42", ((ResultMessage) response.getFirstMessage()).getResultContent());
    }

    @Test
    void testRoundTripSerialization() {
        AssistantMessage originalMsg = AssistantMessage.create("Original message");
        ChatResponse original = ChatResponse.builder()
                .id("test-id")
                .model("gpt-4")
                .messages(List.of(originalMsg))
                .build();

        Map<String, Object> openAI = original.toOpenAIMessage();
        ChatResponse restored = ChatResponse.fromOpenAIFormat(openAI);

        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getModel(), restored.getModel());
        assertEquals(original.getFirstMessage().getRole(), restored.getFirstMessage().getRole());
    }

    @Test
    void testRoundTripWithFunctionCall() {
        AssistantMessage originalMsg = AssistantMessage.createWithFunctionCall("my_function",
                Map.of("arg1", "value1"));
        ChatResponse original = ChatResponse.builder()
                .messages(List.of(originalMsg))
                .finishReason("tool_calls")
                .build();

        Map<String, Object> openAI = original.toOpenAIMessage();
        ChatResponse restored = ChatResponse.fromOpenAIFormat(openAI);

        assertTrue(restored.hasFunctionCall());
        assertEquals("my_function", restored.getAssistantMessage().getFunctionName());
    }

    @Test
    void testFromAnthropicFormat() {
        Map<String, Object> anthropicFormat = Map.of(
                "id", "msg_abc123",
                "model", "claude-3-opus",
                "stop_reason", "end_turn",
                "content", List.of(
                        Map.of("type", "text", "text", "Hello from Claude!"),
                        Map.of("type", "tool_use", "id", "tool_call_1", "name", "get_weather",
                                "input", Map.of("location", "Paris"))
                ),
                "usage", Map.of(
                        "input_tokens", 100,
                        "output_tokens", 50
                )
        );

        ChatResponse response = ChatResponse.fromAnthropicFormat(anthropicFormat);

        assertEquals("msg_abc123", response.getId());
        assertEquals("claude-3-opus", response.getModel());
        assertEquals("end_turn", response.getFinishReason());
        assertEquals(150, response.getUsage().getTotalTokens());
    }

    @Test
    void testFromAnthropicFormatWithToolResult() {
        Map<String, Object> anthropicFormat = Map.of(
                "content", List.of(
                        Map.of("type", "tool_result", "tool_use_id", "call_123", "content", "Result data")
                )
        );

        ChatResponse response = ChatResponse.fromAnthropicFormat(anthropicFormat);

        AssistantMessage assistantMsg = response.getAssistantMessage();
        assertNotNull(assistantMsg);
        assertTrue(assistantMsg.hasToolResult());
    }

    @Test
    void testBuilderAddMessage() {
        ChatResponse response = ChatResponse.builder()
                .addMessage(UserMessage.create("Hello"))
                .addMessage(AssistantMessage.create("Hi"))
                .build();

        assertEquals(2, response.getMessages().size());
    }

    @Test
    void testEmptyMessages() {
        ChatResponse response = ChatResponse.builder().build();

        assertNull(response.getFirstMessage());
        assertNull(response.getAssistantMessage());
        assertFalse(response.hasFunctionCall());
    }

    @Test
    void testToString() {
        ChatResponse response = ChatResponse.builder()
                .id("test-id")
                .model("gpt-4")
                .messages(List.of(AssistantMessage.create("Hello")))
                .build();

        String str = response.toString();
        assertTrue(str.contains("test-id"));
        assertTrue(str.contains("gpt-4"));
    }
}

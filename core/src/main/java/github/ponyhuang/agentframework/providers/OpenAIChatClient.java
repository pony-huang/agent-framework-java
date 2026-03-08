package github.ponyhuang.agentframework.providers;

import github.ponyhuang.agentframework.clients.DefaultChatClient;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Content;
import github.ponyhuang.agentframework.types.Message;
import github.ponyhuang.agentframework.types.Role;
import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionMessage;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionToolMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OpenAI Chat Client implementation.
 *
 * This class keeps the provider integration surface compile-safe against the
 * current openai-java SDK layout.
 */
public class OpenAIChatClient extends DefaultChatClient {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAIChatClient.class);

    private final com.openai.client.OpenAIClient client;

    private OpenAIChatClient(Builder builder) {
        super(builder.model);
        this.client = builder.openAIClient;
    }

    @Override
    public ChatResponse chat(ChatCompleteParams params) {
        LOG.info("OpenAI chat request started, model: {}", resolveModel(params));
        try {
            ChatCompletion completion = client.chat().completions().create(toOpenAIParams(params));
            LOG.info("OpenAI chat request completed, id: {}, model: {}", completion.id(), completion.model());
            return toChatResponse(completion);
        } catch (Exception e) {
            LOG.error("OpenAI chat request failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Flux<ChatResponse> chatStream(ChatCompleteParams params) {
        LOG.info("OpenAI chat stream started, model: {}", resolveModel(params));
        ChatCompletionCreateParams createParams = toOpenAIParams(params);
        return Flux.create(sink -> {
            try (StreamResponse<ChatCompletionChunk> streamResponse =
                         client.chat().completions().createStreaming(createParams)) {
                streamResponse.stream().forEach(chunk -> sink.next(toChatResponse(chunk)));
                sink.complete();
                LOG.info("OpenAI chat stream completed");
            } catch (Throwable t) {
                LOG.error("OpenAI chat stream failed: {}", t.getMessage());
                sink.error(t);
            }
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    private ChatCompletionCreateParams toOpenAIParams(ChatCompleteParams params) {
        ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder();
        builder.model(resolveModel(params));

        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature());
        }
        if (params.getTopP() != null) {
            builder.topP(params.getTopP());
        }
        if (params.getMaxTokens() != null) {
            builder.maxCompletionTokens(params.getMaxTokens().longValue());
        }
        if (params.getPresencePenalty() != null) {
            builder.presencePenalty(params.getPresencePenalty());
        }
        if (params.getFrequencyPenalty() != null) {
            builder.frequencyPenalty(params.getFrequencyPenalty());
        }
        if (params.getN() != null) {
            builder.n(params.getN().longValue());
        }
        if (params.getStop() != null && !params.getStop().isBlank()) {
            builder.stop(params.getStop());
        }

        if (params.getMessages() != null) {
            for (Message message : params.getMessages()) {
                ChatCompletionMessageParam converted = toOpenAIMessage(message);
                if (converted != null) {
                    builder.addMessage(converted);
                }
            }
        }

        if (params.getSystem() != null && !params.getSystem().isBlank() && !containsSystemMessage(params)) {
            builder.addMessage(ChatCompletionMessageParam.ofSystem(
                    ChatCompletionSystemMessageParam.builder().content(params.getSystem()).build()));
        }

        if (params.getTools() != null) {
            for (Map<String, Object> tool : params.getTools()) {
                ChatCompletionFunctionTool functionTool = toOpenAIFunctionTool(tool);
                if (functionTool != null) {
                    builder.addTool(functionTool);
                }
            }
        }

        return builder.build();
    }

    private ChatCompletionMessageParam toOpenAIMessage(Message message) {
        if (message == null || message.getRole() == null) {
            return null;
        }

        switch (message.getRole()) {
            case SYSTEM -> {
                return ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder()
                                .content(message.getText())
                                .build());
            }
            case USER -> {
                return ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(message.getText())
                                .build());
            }
            case ASSISTANT -> {
                com.openai.models.chat.completions.ChatCompletionAssistantMessageParam.Builder assistantBuilder =
                        com.openai.models.chat.completions.ChatCompletionAssistantMessageParam.builder();
                if (!message.getText().isBlank()) {
                    assistantBuilder.content(message.getText());
                }
                boolean hasToolCall = false;
                Map<String, Object> functionCall = message.getFunctionCall();
                if (functionCall != null) {
                    hasToolCall = true;
                    ChatCompletionMessageFunctionToolCall.Function function =
                            ChatCompletionMessageFunctionToolCall.Function.builder()
                                    .name(asNonBlankString(functionCall.get("name")).orElse("tool"))
                                    .arguments(toJsonString(functionCall.get("arguments")))
                                    .build();
                    assistantBuilder.addToolCall(
                            ChatCompletionMessageFunctionToolCall.builder()
                                    .id(asNonBlankString(functionCall.get("id"))
                                            .orElseGet(() -> "call_" + UUID.randomUUID()))
                                    .function(function)
                                    .build());
                }
                if (hasToolCall) {
                    assistantBuilder.putAdditionalProperty("reasoning_content", JsonValue.from(""));
                }
                return ChatCompletionMessageParam.ofAssistant(assistantBuilder.build());
            }
            case TOOL -> {
                String toolCallId = resolveToolCallId(message);
                if (toolCallId == null || toolCallId.isBlank()) {
                    toolCallId = "tool_" + UUID.randomUUID();
                }
                return ChatCompletionMessageParam.ofTool(
                        ChatCompletionToolMessageParam.builder()
                                .toolCallId(toolCallId)
                                .content(extractToolResultText(message))
                                .build());
            }
            default -> {
                return null;
            }
        }
    }

    private ChatCompletionFunctionTool toOpenAIFunctionTool(Map<String, Object> tool) {
        if (tool == null) {
            return null;
        }

        Map<String, Object> functionBody = asMap(tool.get("function")).orElse(tool);
        String name = asNonBlankString(functionBody.get("name")).orElse(null);
        if (name == null) {
            return null;
        }

        FunctionDefinition.Builder functionBuilder = FunctionDefinition.builder().name(name);
        asNonBlankString(functionBody.get("description")).ifPresent(functionBuilder::description);

        asMap(functionBody.get("parameters")).ifPresent(parameters -> {
            FunctionParameters.Builder parametersBuilder = FunctionParameters.builder();
            parameters.forEach((key, value) -> parametersBuilder.putAdditionalProperty(key, JsonValue.from(value)));
            functionBuilder.parameters(parametersBuilder.build());
        });

        return ChatCompletionFunctionTool.builder().function(functionBuilder.build()).build();
    }

    private ChatResponse toChatResponse(ChatCompletion completion) {
        List<ChatResponse.Choice> choices = new ArrayList<>();
        for (ChatCompletion.Choice choice : completion.choices()) {
            Message message = toAgentMessage(choice.message());
            String finishReason = choice.finishReason().asString();
            choices.add(new ChatResponse.Choice(Math.toIntExact(choice.index()), message, finishReason));
        }

        ChatResponse.Usage usage = completion.usage()
                .map(u -> new ChatResponse.Usage(
                        Math.toIntExact(u.promptTokens()),
                        Math.toIntExact(u.completionTokens()),
                        Math.toIntExact(u.totalTokens())))
                .orElse(null);

        return ChatResponse.builder()
                .id(completion.id())
                .created(completion.created())
                .model(completion.model())
                .choices(choices)
                .usage(usage)
                .finishReason(choices.isEmpty() ? null : choices.get(0).getFinishReason())
                .build();
    }

    private ChatResponse toChatResponse(ChatCompletionChunk chunk) {
        List<ChatResponse.Choice> choices = new ArrayList<>();
        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            Message message = toAgentMessage(choice.delta());
            String finishReason = choice.finishReason().map(ChatCompletionChunk.Choice.FinishReason::asString).orElse(null);
            choices.add(new ChatResponse.Choice(Math.toIntExact(choice.index()), message, finishReason));
        }

        ChatResponse.Usage usage = chunk.usage()
                .map(u -> new ChatResponse.Usage(
                        Math.toIntExact(u.promptTokens()),
                        Math.toIntExact(u.completionTokens()),
                        Math.toIntExact(u.totalTokens())))
                .orElse(null);

        String finishReason = null;
        for (ChatResponse.Choice choice : choices) {
            if (choice.getFinishReason() != null) {
                finishReason = choice.getFinishReason();
                break;
            }
        }

        return ChatResponse.builder()
                .id(chunk.id())
                .created(chunk.created())
                .model(chunk.model())
                .choices(choices)
                .usage(usage)
                .finishReason(finishReason)
                .build();
    }

    private Message toAgentMessage(ChatCompletionMessage message) {
        Message.Builder builder = Message.builder().role(Role.ASSISTANT);
        final boolean[] hasText = {false};
        message.content().ifPresent(text -> {
            if (!text.isBlank()) {
                builder.addContent(Content.text(text));
                hasText[0] = true;
            }
        });
        if (!hasText[0]) {
            String reasoning = extractReasoningContent(message._additionalProperties());
            if (reasoning != null && !reasoning.isBlank()) {
                builder.addContent(Content.text(reasoning));
            }
        }
        appendToolCalls(builder, message.toolCalls());
        message.functionCall().ifPresent(functionCall -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", functionCall.name());
            payload.put("arguments", parseJsonObject(functionCall.arguments()));
            builder.addContent(Content.fromFunctionCall(payload));
        });
        return builder.build();
    }

    private Message toAgentMessage(ChatCompletionChunk.Choice.Delta delta) {
        Message.Builder builder = Message.builder()
                .role(delta.role().map(role -> toAgentRole(role.asString())).orElse(Role.ASSISTANT));

        delta.content().ifPresent(text -> {
            if (!text.isBlank()) {
                builder.addContent(Content.text(text));
            }
        });

        if (delta.toolCalls().isPresent()) {
            for (ChatCompletionChunk.Choice.Delta.ToolCall toolCall : delta.toolCalls().get()) {
                Map<String, Object> payload = new LinkedHashMap<>();
                toolCall.id().ifPresent(id -> payload.put("id", id));
                toolCall.function().ifPresent(fn -> {
                    fn.name().ifPresent(name -> payload.put("name", name));
                    fn.arguments().ifPresent(args -> payload.put("arguments", parseJsonObject(args)));
                });
                if (!payload.isEmpty()) {
                    builder.addContent(Content.fromFunctionCall(payload));
                }
            }
        }

        delta.functionCall().ifPresent(functionCall -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            functionCall.name().ifPresent(name -> payload.put("name", name));
            functionCall.arguments().ifPresent(args -> payload.put("arguments", parseJsonObject(args)));
            builder.addContent(Content.fromFunctionCall(payload));
        });

        return builder.build();
    }

    private void appendToolCalls(Message.Builder builder, Optional<List<com.openai.models.chat.completions.ChatCompletionMessageToolCall>> toolCalls) {
        if (toolCalls.isEmpty()) {
            return;
        }
        for (com.openai.models.chat.completions.ChatCompletionMessageToolCall toolCall : toolCalls.get()) {
            if (toolCall.isFunction()) {
                ChatCompletionMessageFunctionToolCall functionToolCall = toolCall.asFunction();
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("id", functionToolCall.id());
                payload.put("name", functionToolCall.function().name());
                payload.put("arguments", parseJsonObject(functionToolCall.function().arguments()));
                builder.addContent(Content.fromFunctionCall(payload));
            }
        }
    }

    private Role toAgentRole(String role) {
        if ("user".equalsIgnoreCase(role)) {
            return Role.USER;
        }
        if ("system".equalsIgnoreCase(role) || "developer".equalsIgnoreCase(role)) {
            return Role.SYSTEM;
        }
        if ("tool".equalsIgnoreCase(role)) {
            return Role.TOOL;
        }
        return Role.ASSISTANT;
    }

    private String resolveModel(ChatCompleteParams params) {
        String resolved = params.getModel() != null ? params.getModel() : this.model;
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalStateException("Model is required. Set it via builder.model(...) or params.model(...).");
        }
        return resolved;
    }

    private boolean containsSystemMessage(ChatCompleteParams params) {
        if (params.getMessages() == null) {
            return false;
        }
        for (Message message : params.getMessages()) {
            if (message != null && message.getRole() == Role.SYSTEM) {
                return true;
            }
        }
        return false;
    }

    private Optional<Map<String, Object>> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    result.put(key, entry.getValue());
                }
            }
            return Optional.of(result);
        }
        return Optional.empty();
    }

    private Optional<String> asNonBlankString(Object value) {
        if (value instanceof String s && !s.isBlank()) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    private String extractToolResultText(Message message) {
        if (message.getContents() == null) {
            return "";
        }
        for (Content content : message.getContents()) {
            if (content.getType() == Content.ContentType.FUNCTION_RESULT) {
                Map<String, Object> functionResult = content.getFunctionResult();
                if (functionResult == null || functionResult.isEmpty()) {
                    return "";
                }
                Object result = functionResult.getOrDefault("result", functionResult);
                return toJsonString(result);
            }
            if (content.getType() == Content.ContentType.TEXT && content.getText() != null) {
                return content.getText();
            }
        }
        return message.getText();
    }

    private String resolveToolCallId(Message message) {
        if (message.getToolCallId() != null && !message.getToolCallId().isBlank()) {
            return message.getToolCallId();
        }
        if (message.getContents() != null) {
            for (Content content : message.getContents()) {
                if (content.getToolCallId() != null && !content.getToolCallId().isBlank()) {
                    return content.getToolCallId();
                }
            }
        }
        return null;
    }

    private Object parseJsonObject(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<String, Object>();
        }
        try {
            return JsonCodec.MAPPER.readValue(raw, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("__raw", raw);
            return fallback;
        }
    }

    private String extractReasoningContent(Map<String, com.openai.core.JsonValue> additionalProperties) {
        if (additionalProperties == null || additionalProperties.isEmpty()) {
            return null;
        }
        com.openai.core.JsonValue value = additionalProperties.get("reasoning_content");
        if (value == null) {
            return null;
        }
        try {
            return value.convert(String.class);
        } catch (Exception e) {
            return value.toString();
        }
    }

    private String toJsonString(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return JsonCodec.MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private static final class JsonCodec {
        private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER =
                new com.fasterxml.jackson.databind.ObjectMapper();
    }

    public static class Builder {
        private com.openai.client.OpenAIClient openAIClient;
        private String apiKey;
        private String baseUrl;
        private String model;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder client(com.openai.client.OpenAIClient client) {
            this.openAIClient = client;
            return this;
        }

        public OpenAIChatClient build() {
            if (openAIClient == null) {
                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalStateException("OpenAI client is required. Use apiKey() or client() to set it.");
                }

                com.openai.client.okhttp.OpenAIOkHttpClient.Builder sdkBuilder =
                        com.openai.client.okhttp.OpenAIOkHttpClient.builder().apiKey(apiKey);
                if (baseUrl != null && !baseUrl.isBlank()) {
                    sdkBuilder.baseUrl(baseUrl);
                }
                openAIClient = sdkBuilder.build();
            }

            return new OpenAIChatClient(this);
        }
    }
}

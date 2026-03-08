package github.ponyhuang.agentframework.providers;

import com.anthropic.core.JsonValue;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.*;
import github.ponyhuang.agentframework.clients.DefaultChatClient;
import github.ponyhuang.agentframework.types.*;
import github.ponyhuang.agentframework.types.Message;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

/**
 * Anthropic Chat Client implementation.
 * <p>
 * This class keeps the provider integration surface compile-safe against the
 * current anthropic-java SDK layout.
 */
public class AnthropicChatClient extends DefaultChatClient {

    private static final Logger LOG = LoggerFactory.getLogger(AnthropicChatClient.class);

    private final com.anthropic.client.AnthropicClient client;

    private AnthropicChatClient(Builder builder) {
        super(builder.model);
        this.client = builder.anthropicClient;
    }

    @Override
    public ChatResponse chat(ChatCompleteParams params) {
        LOG.info("Anthropic chat request started, model: {}", resolveModel(params));
        try {
            com.anthropic.models.messages.Message message = client.messages().create(toAnthropicParams(params));
            LOG.info("Anthropic chat request completed, id: {}, model: {}", message.id(), message.model().asString());
            return toChatResponse(message);
        } catch (Exception e) {
            LOG.error("Anthropic chat request failed: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Flux<ChatResponse> chatStream(ChatCompleteParams params) {
        LOG.info("Anthropic chat stream started, model: {}", resolveModel(params));
        MessageCreateParams createParams = toAnthropicParams(params);
        return Flux.create(sink -> {
            try (StreamResponse<RawMessageStreamEvent> streamResponse =
                         client.messages().createStreaming(createParams)) {
                streamResponse.stream().forEach(event -> {
                    ChatResponse mapped = toChatResponse(event);
                    if (mapped != null) {
                        sink.next(mapped);
                    }
                });
                sink.complete();
                LOG.info("Anthropic chat stream completed");
            } catch (Throwable t) {
                LOG.error("Anthropic chat stream failed: {}", t.getMessage());
                sink.error(t);
            }
        });
    }

    public static Builder builder() {
        return new Builder();
    }

    private MessageCreateParams toAnthropicParams(ChatCompleteParams params) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder();
        builder.model(resolveModel(params));
        builder.maxTokens(params.getMaxTokens() != null ? params.getMaxTokens().longValue() : 1024L);

        if (params.getTemperature() != null) {
            builder.temperature(params.getTemperature());
        }
        if (params.getTopP() != null) {
            builder.topP(params.getTopP());
        }
        if (params.getStop() != null && !params.getStop().isBlank()) {
            builder.addStopSequence(params.getStop());
        }

        String systemPrompt = params.getEffectiveSystem();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            builder.system(systemPrompt);
        }

        if (params.getMessages() != null) {
            for (Message message : params.getMessages()) {
                MessageParam messageParam = toAnthropicMessage(message);
                if (messageParam != null) {
                    builder.addMessage(messageParam);
                }
            }
        }

        if (params.getTools() != null) {
            for (Map<String, Object> tool : params.getTools()) {
                Tool convertedTool = toAnthropicTool(tool);
                if (convertedTool != null) {
                    builder.addTool(convertedTool);
                }
            }
        }

        return builder.build();
    }

    private MessageParam toAnthropicMessage(Message message) {
        if (message == null || message.getRole() == null) {
            return null;
        }

        switch (message.getRole()) {
            case USER -> {
                return MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(message.getText())
                        .build();
            }
            case ASSISTANT -> {
                List<ContentBlockParam> blocks = buildAssistantContentBlocks(message);
                if (!blocks.isEmpty()) {
                    return MessageParam.builder()
                            .role(MessageParam.Role.ASSISTANT)
                            .contentOfBlockParams(blocks)
                            .build();
                }
                return MessageParam.builder()
                        .role(MessageParam.Role.ASSISTANT)
                        .content(message.getText())
                        .build();
            }
            case TOOL -> {
                ToolResultBlockParam toolResult = ToolResultBlockParam.builder()
                        .toolUseId(resolveToolCallId(message))
                        .content(extractToolResultText(message))
                        .build();
                return MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .contentOfBlockParams(List.of(ContentBlockParam.ofToolResult(toolResult)))
                        .build();
            }
            default -> {
                return null;
            }
        }
    }

    private List<ContentBlockParam> buildAssistantContentBlocks(Message message) {
        if (message.getContents() == null || message.getContents().isEmpty()) {
            return List.of();
        }
        List<ContentBlockParam> blocks = new ArrayList<>();
        for (Content content : message.getContents()) {
            if (content == null || content.getType() == null) {
                continue;
            }
            if (content.getType() == Content.ContentType.TEXT) {
                String text = content.getText();
                if (text != null && !text.isBlank()) {
                    blocks.add(ContentBlockParam.ofText(TextBlockParam.builder().text(text).build()));
                }
                continue;
            }
            if (content.getType() == Content.ContentType.FUNCTION_CALL) {
                Map<String, Object> call = content.getFunctionCall();
                if (call == null || call.isEmpty()) {
                    continue;
                }
                String name = asNonBlankString(call.get("name")).orElse(null);
                if (name == null) {
                    continue;
                }
                String id = asNonBlankString(call.get("id")).orElseGet(() -> {
                    String generated = "tool_" + UUID.randomUUID();
                    call.put("id", generated);
                    return generated;
                });

                @SuppressWarnings("unchecked")
                Map<String, Object> args = call.get("arguments") instanceof Map<?, ?>
                        ? (Map<String, Object>) call.get("arguments")
                        : null;
                ToolUseBlockParam.Input.Builder inputBuilder = ToolUseBlockParam.Input.builder();
                if (args != null) {
                    for (Map.Entry<String, Object> entry : args.entrySet()) {
                        inputBuilder.putAdditionalProperty(entry.getKey(), JsonValue.from(entry.getValue()));
                    }
                }

                ToolUseBlockParam toolUse = ToolUseBlockParam.builder()
                        .id(id)
                        .name(name)
                        .input(inputBuilder.build())
                        .build();
                blocks.add(ContentBlockParam.ofToolUse(toolUse));
            }
        }
        return blocks;
    }

    private Tool toAnthropicTool(Map<String, Object> toolMap) {
        if (toolMap == null) {
            return null;
        }
        Map<String, Object> tool = asMap(toolMap.get("function")).orElse(toolMap);
        String name = asNonBlankString(tool.get("name")).orElse(null);
        if (name == null) {
            return null;
        }

        Tool.Builder builder = Tool.builder().name(name);
        asNonBlankString(tool.get("description")).ifPresent(builder::description);

        Tool.InputSchema.Builder schemaBuilder = Tool.InputSchema.builder();
        asMap(tool.get("parameters")).ifPresent(parameters -> {
            Object type = parameters.get("type");
            if (type != null) {
                schemaBuilder.type(JsonValue.from(type));
            }

            Map<String, Object> properties = asMap(parameters.get("properties")).orElse(null);
            if (properties == null && !parameters.containsKey("type")
                    && !parameters.containsKey("required") && !parameters.containsKey("properties")) {
                properties = parameters;
            }
            if (properties != null) {
                Tool.InputSchema.Properties.Builder propertiesBuilder = Tool.InputSchema.Properties.builder();
                properties.forEach((k, v) -> propertiesBuilder.putAdditionalProperty(k, JsonValue.from(v)));
                schemaBuilder.properties(propertiesBuilder.build());
                if (type == null) {
                    schemaBuilder.type(JsonValue.from("object"));
                }
            }

            Object required = parameters.get("required");
            if (required instanceof List<?> requiredList) {
                for (Object item : requiredList) {
                    if (item instanceof String key && !key.isBlank()) {
                        schemaBuilder.addRequired(key);
                    }
                }
            }

            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String key = entry.getKey();
                if ("type".equals(key) || "properties".equals(key) || "required".equals(key)) {
                    continue;
                }
                schemaBuilder.putAdditionalProperty(key, JsonValue.from(entry.getValue()));
            }
        });
        builder.inputSchema(schemaBuilder.build());

        return builder.build();
    }

    private ChatResponse toChatResponse(com.anthropic.models.messages.Message message) {
        Message assistantMessage = toAgentMessage(message);
        ChatResponse.Choice choice = new ChatResponse.Choice(
                0,
                assistantMessage,
                message.stopReason().map(com.anthropic.models.messages.StopReason::asString).orElse(null));

        int promptTokens = Math.toIntExact(message.usage().inputTokens());
        int completionTokens = Math.toIntExact(message.usage().outputTokens());
        ChatResponse.Usage usage = new ChatResponse.Usage(
                promptTokens,
                completionTokens,
                promptTokens + completionTokens);

        return ChatResponse.builder()
                .id(message.id())
                .created(Instant.now().getEpochSecond())
                .model(message.model().asString())
                .choices(List.of(choice))
                .usage(usage)
                .finishReason(choice.getFinishReason())
                .build();
    }

    private ChatResponse toChatResponse(RawMessageStreamEvent event) {
        if (event.isMessageStart()) {
            return toChatResponse(event.asMessageStart().message());
        }

        if (event.isContentBlockDelta()) {
            RawContentBlockDeltaEvent deltaEvent = event.asContentBlockDelta();
            String text = extractDeltaText(deltaEvent);
            if (text == null || text.isBlank()) {
                return null;
            }
            Message chunkMessage = Message.builder()
                    .role(Role.ASSISTANT)
                    .addContent(Content.text(text))
                    .build();
            ChatResponse.Choice choice = new ChatResponse.Choice(0, chunkMessage, null);
            return ChatResponse.builder()
                    .created(Instant.now().getEpochSecond())
                    .choices(List.of(choice))
                    .build();
        }

        if (event.isMessageDelta()) {
            RawMessageDeltaEvent deltaEvent = event.asMessageDelta();
            String finishReason = deltaEvent.delta().stopReason()
                    .map(com.anthropic.models.messages.StopReason::asString)
                    .orElse(null);
            int completionTokens = Math.toIntExact(deltaEvent.usage().outputTokens());
            ChatResponse.Usage usage = new ChatResponse.Usage(0, completionTokens, completionTokens);
            ChatResponse.Choice choice = new ChatResponse.Choice(
                    0,
                    Message.builder().role(Role.ASSISTANT).build(),
                    finishReason);
            return ChatResponse.builder()
                    .created(Instant.now().getEpochSecond())
                    .choices(List.of(choice))
                    .usage(usage)
                    .finishReason(finishReason)
                    .build();
        }

        return null;
    }

    private Message toAgentMessage(com.anthropic.models.messages.Message message) {
        Message.Builder builder = Message.builder().role(Role.ASSISTANT);
        for (ContentBlock block : message.content()) {
            if (block.isText()) {
                String text = block.asText().text();
                if (!text.isBlank()) {
                    builder.addContent(Content.text(text));
                }
            } else if (block.isToolUse()) {
                Map<String, Object> functionCall = new LinkedHashMap<>();
                functionCall.put("id", block.asToolUse().id());
                functionCall.put("name", block.asToolUse().name());
                functionCall.put("arguments", block.asToolUse()._input().convert(Map.class));
                builder.addContent(Content.fromFunctionCall(functionCall));
            }
        }
        return builder.build();
    }

    private String extractDeltaText(RawContentBlockDeltaEvent deltaEvent) {
        if (deltaEvent.delta().isText()) {
            return deltaEvent.delta().asText().text();
        }
        if (deltaEvent.delta().isThinking()) {
            return deltaEvent.delta().asThinking().thinking();
        }
        return null;
    }

    private String resolveModel(ChatCompleteParams params) {
        String resolved = params.getModel() != null ? params.getModel() : this.model;
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalStateException("Model is required. Set it via builder.model(...) or params.model(...).");
        }
        return resolved;
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
            return message.getText();
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
        return "tool_" + UUID.randomUUID();
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
        private com.anthropic.client.AnthropicClient anthropicClient;
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

        public Builder client(com.anthropic.client.AnthropicClient client) {
            this.anthropicClient = client;
            return this;
        }

        public AnthropicChatClient build() {
            if (anthropicClient == null) {
                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalStateException("Anthropic client is required. Use apiKey() or client() to set it.");
                }

                com.anthropic.client.okhttp.AnthropicOkHttpClient.Builder sdkBuilder =
                        com.anthropic.client.okhttp.AnthropicOkHttpClient.builder().apiKey(apiKey);
                if (baseUrl != null && !baseUrl.isBlank()) {
                    sdkBuilder.baseUrl(baseUrl);
                }
                anthropicClient = sdkBuilder.build();
            }

            return new AnthropicChatClient(this);
        }
    }
}

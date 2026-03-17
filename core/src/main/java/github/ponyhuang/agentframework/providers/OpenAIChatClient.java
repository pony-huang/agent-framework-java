package github.ponyhuang.agentframework.providers;

import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Role;
import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.TextBlock;
import github.ponyhuang.agentframework.types.block.ToolResultBlock;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * OpenAI Chat Client implementation.
 * <p>
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
            AccumulatorState state = new AccumulatorState();

            try (StreamResponse<ChatCompletionChunk> streamResponse =
                         client.chat().completions().createStreaming(createParams)) {
                streamResponse.stream().forEach(chunk -> {
                    accumulate(state, chunk);
                    ChatResponse response = toChatResponse(state);

                    // Only emit if there's new content or usage update
                    if (!response.getBlocks().isEmpty() || response.getUsage() != null) {
                        sink.next(response);

                        // Clear blocks after emitting to avoid duplicates
                        state.blocks.clear();
                    }
                });
                sink.complete();
                LOG.info("OpenAI chat stream completed");
            } catch (Throwable t) {
                LOG.error("OpenAI chat stream failed: {}", t.getMessage());
                sink.error(t);
            }
        });
    }

    private static class AccumulatorState {
        String id;
        long created;
        String model;
        List<Block> blocks = new ArrayList<>();
        ChatResponse.Usage usage;
        String finishReason;

        // Current building content
        StringBuilder currentText = new StringBuilder();
        StringBuilder currentReasoning = new StringBuilder();

        // Current tool call being built
        String currentToolCallId;
        String currentToolCallName;
        StringBuilder currentToolCallArguments = new StringBuilder();

        // Legacy functionCall support
        StringBuilder currentFunctionCallName = new StringBuilder();
        StringBuilder currentFunctionCallArguments = new StringBuilder();
    }

    private void accumulate(AccumulatorState acc, ChatCompletionChunk chunk) {
        // Set basic info from first chunk
        if (acc.id == null && chunk.id() != null) {
            acc.id = chunk.id();
            acc.created = chunk.created();
            acc.model = chunk.model();
        }

        for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            ChatCompletionChunk.Choice.Delta delta = choice.delta();

            // Accumulate text content
            delta.content().ifPresent(text -> acc.currentText.append(text));

            // Accumulate reasoning content
            String reasoning = extractReasoningContent(delta._additionalProperties());
            if (reasoning != null) {
                acc.currentReasoning.append(reasoning);
            }

            // Handle tool calls
            if (delta.toolCalls().isPresent()) {
                for (ChatCompletionChunk.Choice.Delta.ToolCall toolCall : delta.toolCalls().get()) {
                    // Start new tool call or continue existing one
                    toolCall.id().ifPresent(id -> {
                        if (!id.equals(acc.currentToolCallId)) {
                            // Flush previous tool call if exists
                            flushToolCall(acc);
                            acc.currentToolCallId = id;
                        }
                    });

                    toolCall.function().ifPresent(fn -> {
                        fn.name().ifPresent(name -> acc.currentToolCallName = name);
                        fn.arguments().ifPresent(args -> acc.currentToolCallArguments.append(args));
                    });
                }
            }

            // Handle legacy functionCall
            delta.functionCall().ifPresent(functionCall -> {
                functionCall.name().ifPresent(name -> {
                    if (!name.equals(acc.currentFunctionCallName.toString())) {
                        acc.currentFunctionCallName = new StringBuilder(name);
                    }
                });
                functionCall.arguments().ifPresent(args -> acc.currentFunctionCallArguments.append(args));
            });

            // Update finish reason
            if (choice.finishReason().isPresent()) {
                acc.finishReason = choice.finishReason().get().asString();
            }
        }

        // Update usage (may come in later chunks)
        chunk.usage().ifPresent(u -> {
            acc.usage = new ChatResponse.Usage(
                    Math.toIntExact(u.promptTokens()),
                    Math.toIntExact(u.completionTokens()),
                    Math.toIntExact(u.totalTokens()));
        });

        // Flush accumulated content to blocks
        flushContent(acc);
    }

    private void flushContent(AccumulatorState acc) {
        boolean hasContent = false;

        // Flush text content
        if (!acc.currentText.isEmpty()) {
            acc.blocks.add(TextBlock.of(acc.currentText.toString()));
            acc.currentText = new StringBuilder();
            hasContent = true;
        }

        // Flush reasoning content (only if no text or separate block)
        if (!acc.currentReasoning.isEmpty()) {
            // For reasoning, we could add a separate block or append to text
            // Adding as separate block for clarity
            acc.blocks.add(TextBlock.of(acc.currentReasoning.toString()));
            acc.currentReasoning = new StringBuilder();
            hasContent = true;
        }

        // Flush tool call if complete
        if (acc.currentToolCallId != null || !acc.currentToolCallArguments.isEmpty()) {
            flushToolCall(acc);
            hasContent = true;
        }

        // Flush legacy functionCall
        if (!acc.currentFunctionCallName.isEmpty() || !acc.currentFunctionCallArguments.isEmpty()) {
            Map<String, Object> argsMap = parseJsonObjectSafe(acc.currentFunctionCallArguments.toString());
            acc.blocks.add(ToolUseBlock.of(
                    "temp",
                    acc.currentFunctionCallName.toString(),
                    argsMap));
            acc.currentFunctionCallName = new StringBuilder();
            acc.currentFunctionCallArguments = new StringBuilder();
            hasContent = true;
        }
    }

    private void flushToolCall(AccumulatorState acc) {
        if (acc.currentToolCallId == null && acc.currentToolCallArguments.isEmpty()) {
            return;
        }

        Map<String, Object> argsMap = parseJsonObjectSafe(acc.currentToolCallArguments.toString());
        acc.blocks.add(ToolUseBlock.of(
                acc.currentToolCallId != null ? acc.currentToolCallId : "tool_" + UUID.randomUUID(),
                acc.currentToolCallName != null ? acc.currentToolCallName : "",
                argsMap));

        acc.currentToolCallId = null;
        acc.currentToolCallName = null;
        acc.currentToolCallArguments = new StringBuilder();
    }

    private Map<String, Object> parseJsonObjectSafe(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return JsonCodec.MAPPER.readValue(raw, Map.class);
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("__raw", raw);
            return fallback;
        }
    }

    private ChatResponse toChatResponse(AccumulatorState acc) {
        return ChatResponse.builder()
                .id(acc.id != null ? acc.id : "")
                .created(acc.created)
                .model(acc.model != null ? acc.model : "")
                .messages(List.of())
                .blocks(new ArrayList<>(acc.blocks))
                .usage(acc.usage)
                .finishReason(acc.finishReason != null ? acc.finishReason : "")
                .build();
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
        if (message == null || message.getRoleAsString() == null) {
            return null;
        }

        String role = message.getRoleAsString();

        switch (role) {
            case "system", "SYSTEM" -> {
                return ChatCompletionMessageParam.ofSystem(
                        ChatCompletionSystemMessageParam.builder()
                                .content(message.getTextContent())
                                .build());
            }
            case "user", "USER" -> {
                return ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder()
                                .content(message.getTextContent())
                                .build());
            }
            case "assistant", "ASSISTANT" -> {
                ChatCompletionAssistantMessageParam.Builder assistantBuilder =
                        ChatCompletionAssistantMessageParam.builder();
                if (message.getTextContent() != null && !message.getTextContent().isBlank()) {
                    assistantBuilder.content(message.getTextContent());
                }
                boolean hasToolCall = false;

                if (message instanceof AssistantMessage assistantMsg) {
                    if (assistantMsg.hasFunctionCall()) {
                        hasToolCall = true;

                        // Handle old-style functionCall map
                        Map<String, Object> functionCall = assistantMsg.getFunctionCall();
                        if (functionCall != null) {
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

                        // Handle new-style ToolUseBlock
                        if (message.getBlocks() != null) {
                            for (Block block : message.getBlocks()) {
                                if (block instanceof ToolUseBlock toolUse) {
                                    ChatCompletionMessageFunctionToolCall.Function function =
                                            ChatCompletionMessageFunctionToolCall.Function.builder()
                                                    .name(toolUse.getName())
                                                    .arguments(toolUse.getInput() != null ? toJsonString(toolUse.getInput()) : "{}")
                                                    .build();
                                    assistantBuilder.addToolCall(
                                            ChatCompletionMessageFunctionToolCall.builder()
                                                    .id(toolUse.getId() != null ? toolUse.getId() : "call_" + UUID.randomUUID())
                                                    .function(function)
                                                    .build());
                                }
                            }
                        }
                    }
                }

                if (hasToolCall) {
                    assistantBuilder.putAdditionalProperty("reasoning_content", JsonValue.from(""));
                }
                return ChatCompletionMessageParam.ofAssistant(assistantBuilder.build());
            }
            case "tool", "TOOL" -> {
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
        }

        return null;
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
        List<Message> messages = new ArrayList<>();
        String finishReason = null;
        for (ChatCompletion.Choice choice : completion.choices()) {
            Message message = toAgentMessage(choice.message());
            messages.add(message);
            finishReason = choice.finishReason().asString();
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
                .messages(messages)
                .usage(usage)
                .finishReason(finishReason)
                .build();
    }

    private Message toAgentMessage(ChatCompletionMessage message) {
        List<github.ponyhuang.agentframework.types.block.Block> blocks = new ArrayList<>();

        message.content().ifPresent(text -> {
            if (!text.isBlank()) {
                blocks.add(new TextBlock(text));
            }
        });

        String reasoning = extractReasoningContent(message._additionalProperties());
        if ((blocks.isEmpty() || blocks.stream().noneMatch(b -> b instanceof TextBlock))
                && reasoning != null && !reasoning.isBlank()) {
            blocks.add(new TextBlock(reasoning));
        }

        appendToolCalls(blocks, message.toolCalls());

        message.functionCall().ifPresent(functionCall -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("name", functionCall.name());
            payload.put("arguments", parseJsonObject(functionCall.arguments()));
            blocks.add(ToolUseBlock.of("temp", functionCall.name(), (Map<String, Object>) parseJsonObject(functionCall.arguments())));
        });

        return AssistantMessage.fromBlocks(blocks);
    }

    private void appendToolCalls(List<github.ponyhuang.agentframework.types.block.Block> blocks, Optional<List<com.openai.models.chat.completions.ChatCompletionMessageToolCall>> toolCalls) {
        if (toolCalls.isEmpty()) {
            return;
        }
        for (com.openai.models.chat.completions.ChatCompletionMessageToolCall toolCall : toolCalls.get()) {
            if (toolCall.isFunction()) {
                ChatCompletionMessageFunctionToolCall functionToolCall = toolCall.asFunction();
                String id = functionToolCall.id();
                String name = functionToolCall.function().name();
                Map<String, Object> argsMap = (Map<String, Object>) parseJsonObject(functionToolCall.function().arguments());
                blocks.add(ToolUseBlock.of(id, name, argsMap));
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
            if (message != null && "system".equalsIgnoreCase(message.getRoleAsString())) {
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
        if (message.getBlocks() == null) {
            return "";
        }
        for (github.ponyhuang.agentframework.types.block.Block block : message.getBlocks()) {
            if (block instanceof ToolResultBlock) {
                ToolResultBlock resultBlock = (ToolResultBlock) block;
                Object result = resultBlock.getContent();
                if (result == null) {
                    return "";
                }
                return toJsonString(result);
            }
            if (block instanceof TextBlock) {
                TextBlock textBlock = (TextBlock) block;
                if (textBlock.getText() != null) {
                    return textBlock.getText();
                }
            }
        }
        return message.getTextContent() != null ? message.getTextContent() : "";
    }

    private String resolveToolCallId(Message message) {
        if (message.getToolCallId() != null && !message.getToolCallId().isBlank()) {
            return message.getToolCallId();
        }
        if (message.getBlocks() != null) {
            for (github.ponyhuang.agentframework.types.block.Block block : message.getBlocks()) {
                if (block instanceof ToolResultBlock) {
                    ToolResultBlock resultBlock = (ToolResultBlock) block;
                    if (resultBlock.getToolUseId() != null && !resultBlock.getToolUseId().isBlank()) {
                        return resultBlock.getToolUseId();
                    }
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

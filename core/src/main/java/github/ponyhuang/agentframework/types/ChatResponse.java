package github.ponyhuang.agentframework.types;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.TextBlock;
import github.ponyhuang.agentframework.types.block.ToolResultBlock;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.ResultMessage;
import github.ponyhuang.agentframework.types.message.UserMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private final String id;
    private final long created;
    private final String model;
    private final List<Message> messages;
    private final List<Block> blocks;
    private final Usage usage;
    private final String finishReason;
    private final Map<String, Object> extraProperties;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ChatResponse(Builder builder) {
        this.id = builder.id;
        this.created = builder.created;
        this.model = builder.model;
        this.messages = builder.messages;
        this.usage = builder.usage;
        this.finishReason = builder.finishReason;
        this.extraProperties = builder.extraProperties;
        this.blocks = builder.blocks;
    }

    public String getId() {
        return id;
    }

    public long getCreated() {
        return created;
    }

    public String getModel() {
        return model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public Usage getUsage() {
        return usage;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public Map<String, Object> getExtraProperties() {
        return extraProperties;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public Message getFirstMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(0);
    }

    /**
     * Gets the last message from the response.
     * This is typically the final assistant response.
     *
     * @return the last message, or null if none
     */
    public Message getLastMessage() {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        return messages.get(messages.size() - 1);
    }

    public Message getMessage() {
        return getLastMessage();
    }

    public AssistantMessage getAssistantMessage() {
        if (messages == null) return null;
        return messages.stream()
                .filter(m -> m instanceof AssistantMessage)
                .map(m -> (AssistantMessage) m)
                .findFirst()
                .orElse(null);
    }

    public boolean hasFunctionCall() {
        if (messages == null) return false;
        return messages.stream()
                .anyMatch(m -> m instanceof AssistantMessage && ((AssistantMessage) m).hasFunctionCall());
    }

    public List<ToolUseBlock> getToolCalls() {
        if (messages == null) return List.of();
        return messages.stream()
                .filter(m -> m instanceof AssistantMessage)
                .flatMap(m -> ((AssistantMessage) m).getBlocks().stream())
                .filter(b -> b instanceof ToolUseBlock)
                .map(b -> (ToolUseBlock) b)
                .collect(Collectors.toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toOpenAIMessage() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("created", created);
        result.put("model", model);

        if (messages != null && !messages.isEmpty()) {
            List<Map<String, Object>> msgList = new ArrayList<>();
            for (Message msg : messages) {
                msgList.add(messageToOpenAI(msg));
            }
            result.put("choices", List.of(Map.of(
                    "index", 0,
                    "message", msgList.get(0),
                    "finish_reason", finishReason != null ? finishReason : "stop"
            )));
        }

        if (usage != null) {
            result.put("usage", Map.of(
                    "prompt_tokens", usage.getPromptTokens(),
                    "completion_tokens", usage.getCompletionTokens(),
                    "total_tokens", usage.getTotalTokens()
            ));
        }

        return result;
    }

    private Map<String, Object> messageToOpenAI(Message msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("role", msg.getRoleAsString());

        if (msg instanceof AssistantMessage assistantMsg) {
            if (assistantMsg.hasFunctionCall()) {
                Map<String, Object> functionCall = new HashMap<>();
                functionCall.put("name", assistantMsg.getFunctionName());
                functionCall.put("arguments", assistantMsg.getFunctionArguments() != null ?
                        assistantMsg.getFunctionArguments().toString() : "{}");
                result.put("function_call", functionCall);
            }
            if (assistantMsg.getTextContent() != null && !assistantMsg.getTextContent().isEmpty()) {
                result.put("content", assistantMsg.getTextContent());
            }
        } else if (msg instanceof ResultMessage resultMsg) {
            result.put("tool_call_id", resultMsg.getToolCallId());
            result.put("content", resultMsg.getResultContent());
        } else {
            String textContent = msg.getTextContent();
            if (textContent != null && !textContent.isEmpty()) {
                result.put("content", textContent);
            }
        }

        return result;
    }

    public static ChatResponse fromOpenAIFormat(Map<String, Object> map) {
        Builder builder = new Builder();

        if (map.containsKey("id")) {
            builder.id((String) map.get("id"));
        }
        if (map.containsKey("created")) {
            Object created = map.get("created");
            if (created instanceof Number) {
                builder.created(((Number) created).longValue());
            }
        }
        if (map.containsKey("model")) {
            builder.model((String) map.get("model"));
        }
        if (map.containsKey("finish_reason")) {
            builder.finishReason((String) map.get("finish_reason"));
        }

        if (map.containsKey("choices")) {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) map.get("choices");
            List<Message> messages = new ArrayList<>();

            for (Map<String, Object> choice : choices) {
                if (choice.containsKey("message")) {
                    Map<String, Object> msgMap = (Map<String, Object>) choice.get("message");
                    messages.add(messageFromOpenAI(msgMap));
                }
            }
            builder.messages(messages);
        }

        if (map.containsKey("usage")) {
            Map<String, Object> usageMap = (Map<String, Object>) map.get("usage");
            builder.usage(new Usage(
                    ((Number) usageMap.getOrDefault("prompt_tokens", 0)).intValue(),
                    ((Number) usageMap.getOrDefault("completion_tokens", 0)).intValue(),
                    ((Number) usageMap.getOrDefault("total_tokens", 0)).intValue()
            ));
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Message messageFromOpenAI(Map<String, Object> map) {
        String role = (String) map.get("role");
        String content = map.containsKey("content") ? (String) map.get("content") : null;
        Map<String, Object> functionCall = map.containsKey("function_call") ?
                (Map<String, Object>) map.get("function_call") : null;

        switch (role) {
            case "user":
                return content != null ? UserMessage.create(content) : UserMessage.create();
            case "system":
                return content != null ?
                        github.ponyhuang.agentframework.types.message.SystemMessage.create(content) :
                        github.ponyhuang.agentframework.types.message.SystemMessage.create();
            case "tool":
                String toolCallId = (String) map.get("tool_call_id");
                return ResultMessage.create(toolCallId, content != null ? content : "");
            case "assistant":
            default:
                AssistantMessage.Builder assistantBuilder = new AssistantMessage.Builder();
                if (content != null && !content.isEmpty()) {
                    assistantBuilder.addBlock(new TextBlock(content));
                }
                if (functionCall != null) {
                    assistantBuilder.functionCall(functionCall);
                }
                return assistantBuilder.build();
        }
    }

    public static ChatResponse fromOpenAIChoice(Map<String, Object> choiceMap) {
        Map<String, Object> messageMap = (Map<String, Object>) choiceMap.get("message");
        Message message = messageFromOpenAI(messageMap);

        Builder builder = new Builder();
        builder.messages(List.of(message));

        if (choiceMap.containsKey("finish_reason")) {
            builder.finishReason((String) choiceMap.get("finish_reason"));
        }

        return builder.build();
    }

    public static ChatResponse fromAnthropicFormat(Map<String, Object> map) {
        Builder builder = new Builder();

        if (map.containsKey("id")) {
            builder.id((String) map.get("id"));
        }
        if (map.containsKey("model")) {
            builder.model((String) map.get("model"));
        }
        if (map.containsKey("stop_reason")) {
            builder.finishReason((String) map.get("stop_reason"));
        }

        if (map.containsKey("content")) {
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) map.get("content");
            List<Block> blocks = new ArrayList<>();

            for (Map<String, Object> content : contentList) {
                String type = (String) content.get("type");
                switch (type) {
                    case "text":
                        blocks.add(new TextBlock((String) content.get("text")));
                        break;
                    case "tool_use":
                        String id = (String) content.get("id");
                        String name = (String) content.get("name");
                        Map<String, Object> input = (Map<String, Object>) content.get("input");
                        blocks.add(ToolUseBlock.of(id, name, input));
                        break;
                    case "tool_result":
                        String toolUseId = (String) content.get("tool_use_id");
                        String resultContent = (String) content.get("content");
                        blocks.add(ToolResultBlock.of(toolUseId, resultContent));
                        break;
                }
            }

            AssistantMessage assistantMessage = AssistantMessage.fromBlocks(blocks);
            builder.messages(List.of(assistantMessage));
        }

        if (map.containsKey("usage")) {
            Map<String, Object> usageMap = (Map<String, Object>) map.get("usage");
            builder.usage(new Usage(
                    ((Number) usageMap.getOrDefault("input_tokens", 0)).intValue(),
                    ((Number) usageMap.getOrDefault("output_tokens", 0)).intValue(),
                    ((Number) usageMap.getOrDefault("input_tokens", 0)).intValue() +
                            ((Number) usageMap.getOrDefault("output_tokens", 0)).intValue()
            ));
        }

        return builder.build();
    }

    public static class Usage {
        private final int promptTokens;
        private final int completionTokens;
        private final int totalTokens;

        public Usage(int promptTokens, int completionTokens, int totalTokens) {
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
        }

        public int getPromptTokens() {
            return promptTokens;
        }

        public int getCompletionTokens() {
            return completionTokens;
        }

        public int getTotalTokens() {
            return totalTokens;
        }
    }

    public static class Builder {
        private String id;
        private long created;
        private String model;
        private List<Message> messages;
        private Usage usage;
        private String finishReason;
        private Map<String, Object> extraProperties;
        private List<Block> blocks;


        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder created(long created) {
            this.created = created;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder addMessage(Message message) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(message);
            return this;
        }

        public Builder usage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        public Builder extraProperties(Map<String, Object> extraProperties) {
            this.extraProperties = extraProperties;
            return this;
        }

        public Builder blocks(List<Block> blocks) {
            this.blocks = blocks;
            return this;
        }


        public ChatResponse build() {
            return new ChatResponse(this);
        }
    }

    @Override
    public String toString() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException ignored) {
        }
        return super.toString();
    }
}

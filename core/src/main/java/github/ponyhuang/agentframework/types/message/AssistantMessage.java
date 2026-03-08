package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.TextBlock;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;

import java.util.List;
import java.util.Map;

public class AssistantMessage extends AbstractMessage {
    private final String content;
    private final Map<String, Object> functionCall;

    private AssistantMessage(String role, List<Block> blocks, String name, String toolCallId,
                             String content, Map<String, Object> functionCall) {
        super(role, blocks, name, toolCallId);
        this.content = content;
        this.functionCall = functionCall;
    }

    public static AssistantMessage create() {
        return new AssistantMessage("assistant", List.of(), null, null, null, null);
    }

    public static AssistantMessage create(String text) {
        return new AssistantMessage("assistant", List.of(new TextBlock(text)), null, null, text, null);
    }

    public static AssistantMessage create(List<Block> blocks) {
        String text = blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .findFirst()
                .orElse(null);
        return new AssistantMessage("assistant", blocks, null, null, text, null);
    }

    public static AssistantMessage createWithFunctionCall(String functionName, Map<String, Object> arguments) {
        ToolUseBlock toolUseBlock = ToolUseBlock.of("temp_id", functionName, arguments);
        return new AssistantMessage("assistant", List.of(toolUseBlock), null, null, null,
                Map.of("name", functionName, "arguments", arguments));
    }

    public static AssistantMessage createWithFunctionCall(Map<String, Object> functionCall) {
        String name = functionCall != null && functionCall.containsKey("name") ?
            (String) functionCall.get("name") : null;
        Object args = functionCall != null && functionCall.containsKey("arguments") ?
            functionCall.get("arguments") : null;
        Map<String, Object> arguments = args instanceof Map ? (Map<String, Object>) args :
            (args != null ? Map.of("arguments", args) : null);
        return createWithFunctionCall("temp_id", name, arguments);
    }

    public static AssistantMessage createWithFunctionCall(String id, String functionName, Map<String, Object> arguments) {
        ToolUseBlock toolUseBlock = ToolUseBlock.of(id, functionName, arguments);
        return new AssistantMessage("assistant", List.of(toolUseBlock), null, null, null,
                Map.of("id", id, "name", functionName, "arguments", arguments));
    }

    public static AssistantMessage fromBlocks(List<Block> blocks) {
        return AssistantMessage.create(blocks);
    }

    public String getContent() {
        return content;
    }

    public Map<String, Object> getFunctionCall() {
        return functionCall;
    }

    public boolean hasFunctionCall() {
        return functionCall != null || hasToolUse();
    }

    public String getFunctionName() {
        if (functionCall != null && functionCall.containsKey("name")) {
            return (String) functionCall.get("name");
        }
        if (hasToolUse()) {
            return getBlocks().stream()
                    .filter(b -> b instanceof ToolUseBlock)
                    .map(b -> ((ToolUseBlock) b).getName())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public String getFunctionCallId() {
        if (functionCall != null && functionCall.containsKey("id")) {
            return (String) functionCall.get("id");
        }
        if (hasToolUse()) {
            return getBlocks().stream()
                    .filter(b -> b instanceof ToolUseBlock)
                    .map(b -> ((ToolUseBlock) b).getId())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public Map<String, Object> getFunctionArguments() {
        if (functionCall != null && functionCall.containsKey("arguments")) {
            return (Map<String, Object>) functionCall.get("arguments");
        }
        if (hasToolUse()) {
            return getBlocks().stream()
                    .filter(b -> b instanceof ToolUseBlock)
                    .map(b -> ((ToolUseBlock) b).getInput())
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public AssistantMessage withText(String text) {
        return new AssistantMessage(this.role,
                java.util.Arrays.asList(new TextBlock(text)),
                this.name, this.toolCallId, text, this.functionCall);
    }

    public AssistantMessage withBlocks(List<Block> blocks) {
        String text = blocks.stream()
                .filter(b -> b instanceof TextBlock)
                .map(b -> ((TextBlock) b).getText())
                .findFirst()
                .orElse(null);
        return new AssistantMessage(this.role, blocks, this.name, this.toolCallId, text, this.functionCall);
    }

    public AssistantMessage withFunctionCall(Map<String, Object> functionCall) {
        return new AssistantMessage(this.role, this.blocks, this.name, this.toolCallId, this.content, functionCall);
    }

    public static class Builder {
        private String role = "assistant";
        private List<Block> blocks;
        private String name;
        private String toolCallId;
        private String content;
        private Map<String, Object> functionCall;

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder blocks(List<Block> blocks) {
            this.blocks = blocks;
            return this;
        }

        public Builder addBlock(Block block) {
            if (this.blocks == null) {
                this.blocks = new java.util.ArrayList<>();
            }
            this.blocks.add(block);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder functionCall(Map<String, Object> functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        public AssistantMessage build() {
            return new AssistantMessage(role, blocks, name, toolCallId, content, functionCall);
        }
    }
}

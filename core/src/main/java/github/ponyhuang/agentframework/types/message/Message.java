package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.Block;

import java.util.List;

public interface Message {
    String getRoleAsString();
    List<Block> getBlocks();
    String getName();
    String getToolCallId();

    default String getRole() {
        return getRoleAsString();
    }

    default String getTextContent() {
        if (getBlocks() == null || getBlocks().isEmpty()) {
            return "";
        }
        return getBlocks().stream()
                .filter(b -> b instanceof github.ponyhuang.agentframework.types.block.TextBlock)
                .map(b -> ((github.ponyhuang.agentframework.types.block.TextBlock) b).getText())
                .findFirst()
                .orElse("");
    }

    default boolean hasToolUse() {
        if (getBlocks() == null) return false;
        return getBlocks().stream().anyMatch(b -> b instanceof github.ponyhuang.agentframework.types.block.ToolUseBlock);
    }

    default boolean hasToolResult() {
        if (getBlocks() == null) return false;
        return getBlocks().stream().anyMatch(b -> b instanceof github.ponyhuang.agentframework.types.block.ToolResultBlock);
    }
}

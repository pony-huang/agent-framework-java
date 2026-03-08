package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.Block;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMessage implements Message {
    protected final String role;
    protected final List<Block> blocks;
    protected final String name;
    protected final String toolCallId;

    protected AbstractMessage(String role, List<Block> blocks, String name, String toolCallId) {
        this.role = role;
        this.blocks = blocks != null ? new ArrayList<>(blocks) : new ArrayList<>();
        this.name = name;
        this.toolCallId = toolCallId;
    }

    @Override
    public String getRoleAsString() {
        return role;
    }

    @Override
    public String getRole() {
        return role;
    }

    @Override
    public List<Block> getBlocks() {
        return blocks;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getToolCallId() {
        return toolCallId;
    }

    @Override
    public String getTextContent() {
        if (blocks == null || blocks.isEmpty()) {
            return "";
        }
        return blocks.stream()
                .filter(b -> b instanceof github.ponyhuang.agentframework.types.block.TextBlock)
                .map(b -> ((github.ponyhuang.agentframework.types.block.TextBlock) b).getText())
                .findFirst()
                .orElse("");
    }

    @Override
    public boolean hasToolUse() {
        if (blocks == null) return false;
        return blocks.stream().anyMatch(b -> b instanceof github.ponyhuang.agentframework.types.block.ToolUseBlock);
    }

    @Override
    public boolean hasToolResult() {
        if (blocks == null) return false;
        return blocks.stream().anyMatch(b -> b instanceof github.ponyhuang.agentframework.types.block.ToolResultBlock);
    }
}

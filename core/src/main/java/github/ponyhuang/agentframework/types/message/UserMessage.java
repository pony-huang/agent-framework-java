package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.TextBlock;

import java.util.List;

public class UserMessage extends AbstractMessage {
    private UserMessage(String role, List<Block> blocks, String name, String toolCallId) {
        super(role, blocks, name, toolCallId);
    }

    public static UserMessage create() {
        return new UserMessage("user", List.of(), null, null);
    }

    public static UserMessage create(String text) {
        return new UserMessage("user", List.of(new TextBlock(text)), null, null);
    }

    public static UserMessage create(List<Block> blocks) {
        return new UserMessage("user", blocks, null, null);
    }

    public static UserMessage create(String text, String name) {
        return new UserMessage("user", List.of(new TextBlock(text)), name, null);
    }

    public static UserMessage fromBlocks(List<Block> blocks) {
        return new UserMessage("user", blocks, null, null);
    }

    public UserMessage withText(String text) {
        return new UserMessage(this.role, List.of(new TextBlock(text)), this.name, this.toolCallId);
    }

    public UserMessage withName(String name) {
        return new UserMessage(this.role, this.blocks, name, this.toolCallId);
    }

    public UserMessage withBlocks(List<Block> blocks) {
        return new UserMessage(this.role, blocks, this.name, this.toolCallId);
    }
}

package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.TextBlock;

import java.util.List;

public class SystemMessage extends AbstractMessage {
    private SystemMessage(String role, List<Block> blocks, String name, String toolCallId) {
        super(role, blocks, name, toolCallId);
    }

    public static SystemMessage create() {
        return new SystemMessage("system", List.of(), null, null);
    }

    public static SystemMessage create(String text) {
        return new SystemMessage("system", List.of(new TextBlock(text)), null, null);
    }

    public static SystemMessage create(List<Block> blocks) {
        return new SystemMessage("system", blocks, null, null);
    }

    public static SystemMessage fromBlocks(List<Block> blocks) {
        return new SystemMessage("system", blocks, null, null);
    }

    public SystemMessage withText(String text) {
        return new SystemMessage(this.role, List.of(new TextBlock(text)), this.name, this.toolCallId);
    }

    public SystemMessage withBlocks(List<Block> blocks) {
        return new SystemMessage(this.role, blocks, this.name, this.toolCallId);
    }
}

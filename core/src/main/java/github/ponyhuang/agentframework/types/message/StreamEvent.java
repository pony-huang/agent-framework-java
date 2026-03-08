package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.TextBlock;

import java.util.List;

public class StreamEvent implements Message {
    private final String role;
    private final List<Block> blocks;
    private final String name;
    private final String toolCallId;
    private final String eventType;
    private final String finishReason;
    private final boolean isDelta;

    public StreamEvent() {
        this.role = "assistant";
        this.blocks = List.of();
        this.name = null;
        this.toolCallId = null;
        this.eventType = "message_delta";
        this.finishReason = null;
        this.isDelta = true;
    }

    public StreamEvent(String role, List<Block> blocks, String name, String toolCallId,
                       String eventType, String finishReason, boolean isDelta) {
        this.role = role;
        this.blocks = blocks != null ? blocks : List.of();
        this.name = name;
        this.toolCallId = toolCallId;
        this.eventType = eventType;
        this.finishReason = finishReason;
        this.isDelta = isDelta;
    }

    public static StreamEvent delta(String text) {
        return new StreamEvent("assistant", List.of(new TextBlock(text)), null, null, "content_delta", null, true);
    }

    public static StreamEvent delta(List<Block> blocks) {
        return new StreamEvent("assistant", blocks, null, null, "content_delta", null, true);
    }

    public static StreamEvent messageStart() {
        return new StreamEvent("assistant", List.of(), null, null, "message_start", null, false);
    }

    public static StreamEvent contentBlockStart(int index) {
        return new StreamEvent("assistant", List.of(), null, null, "content_block_start", null, false);
    }

    public static StreamEvent contentBlockDelta(int index, Block block) {
        return new StreamEvent("assistant", List.of(block), null, null, "content_block_delta", null, true);
    }

    public static StreamEvent contentBlockStop(int index) {
        return new StreamEvent("assistant", List.of(), null, null, "content_block_stop", null, false);
    }

    public static StreamEvent messageDelta(String finishReason) {
        return new StreamEvent("assistant", List.of(), null, null, "message_delta", finishReason, false);
    }

    public static StreamEvent messageStop() {
        return new StreamEvent("assistant", List.of(), null, null, "message_stop", null, false);
    }

    public static StreamEvent of(String role, List<Block> blocks, String eventType) {
        return new StreamEvent(role, blocks, null, null, eventType, null, false);
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

    public String getEventType() {
        return eventType;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public boolean isDelta() {
        return isDelta;
    }

    public boolean isMessageStart() {
        return "message_start".equals(eventType);
    }

    public boolean isMessageDelta() {
        return "message_delta".equals(eventType);
    }

    public boolean isMessageStop() {
        return "message_stop".equals(eventType);
    }

    public boolean isContentBlockStart() {
        return "content_block_start".equals(eventType);
    }

    public boolean isContentBlockDelta() {
        return "content_block_delta".equals(eventType);
    }

    public boolean isContentBlockStop() {
        return "content_block_stop".equals(eventType);
    }

    public String getTextDelta() {
        if (blocks != null && !blocks.isEmpty() && blocks.get(0) instanceof github.ponyhuang.agentframework.types.block.TextBlock) {
            return ((github.ponyhuang.agentframework.types.block.TextBlock) blocks.get(0)).getText();
        }
        return "";
    }

    @Override
    public String toString() {
        return "StreamEvent{role='" + role + "', eventType='" + eventType + "', finishReason='" + finishReason + "', isDelta=" + isDelta + "}";
    }
}

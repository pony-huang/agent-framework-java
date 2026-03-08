package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.TextBlock;
import github.ponyhuang.agentframework.types.block.ThinkingBlock;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreamEventTest {

    @Test
    void testStreamEventMessageStart() {
        StreamEvent event = StreamEvent.messageStart();

        assertEquals("assistant", event.getRole());
        assertEquals("message_start", event.getEventType());
        assertTrue(event.getBlocks().isEmpty());
        assertFalse(event.isDelta());
        assertTrue(event.isMessageStart());
    }

    @Test
    void testStreamEventContentBlockStart() {
        StreamEvent event = StreamEvent.contentBlockStart(0);

        assertEquals("content_block_start", event.getEventType());
        assertFalse(event.isDelta());
        assertTrue(event.isContentBlockStart());
    }

    @Test
    void testStreamEventContentBlockDelta() {
        StreamEvent event = StreamEvent.contentBlockDelta(0, new TextBlock("Hello"));

        assertEquals("content_block_delta", event.getEventType());
        assertTrue(event.isDelta());
        assertTrue(event.isContentBlockDelta());
        assertEquals(1, event.getBlocks().size());
        assertEquals("Hello", event.getTextDelta());
    }

    @Test
    void testStreamEventContentBlockStop() {
        StreamEvent event = StreamEvent.contentBlockStop(0);

        assertEquals("content_block_stop", event.getEventType());
        assertFalse(event.isDelta());
        assertTrue(event.isContentBlockStop());
    }

    @Test
    void testStreamEventMessageDelta() {
        StreamEvent event = StreamEvent.messageDelta("stop");

        assertEquals("message_delta", event.getEventType());
        assertEquals("stop", event.getFinishReason());
        assertFalse(event.isDelta());
        assertTrue(event.isMessageDelta());
    }

    @Test
    void testStreamEventMessageStop() {
        StreamEvent event = StreamEvent.messageStop();

        assertEquals("message_stop", event.getEventType());
        assertFalse(event.isDelta());
        assertTrue(event.isMessageStop());
    }

    @Test
    void testStreamEventDelta() {
        StreamEvent event = StreamEvent.delta("Partial response");

        assertEquals("content_delta", event.getEventType());
        assertTrue(event.isDelta());
        assertEquals("Partial response", event.getTextDelta());
    }

    @Test
    void testStreamEventDeltaWithBlocks() {
        List<github.ponyhuang.agentframework.types.block.Block> blocks = List.of(
                new TextBlock("Part 1"),
                new ThinkingBlock("Thinking...")
        );

        StreamEvent event = StreamEvent.delta(blocks);

        assertEquals("content_delta", event.getEventType());
        assertTrue(event.isDelta());
        assertEquals(2, event.getBlocks().size());
    }

    @Test
    void testStreamEventOf() {
        StreamEvent event = StreamEvent.of("user", List.of(), "custom_event");

        assertEquals("user", event.getRole());
        assertEquals("custom_event", event.getEventType());
    }

    @Test
    void testStreamEventDefaultConstructor() {
        StreamEvent event = new StreamEvent();

        assertEquals("assistant", event.getRole());
        assertEquals("message_delta", event.getEventType());
        assertTrue(event.isDelta());
        assertEquals("", event.getTextDelta());
    }

    @Test
    void testStreamEventWithFullParameters() {
        List<github.ponyhuang.agentframework.types.block.Block> blocks = List.of(
                new ToolUseBlock("call_1", "my_tool", null)
        );
        StreamEvent event = new StreamEvent(
                "assistant",
                blocks,
                "test_name",
                "tool_call_id",
                "tool_use",
                "tool_calls",
                false
        );

        assertEquals("assistant", event.getRole());
        assertEquals("test_name", event.getName());
        assertEquals("tool_call_id", event.getToolCallId());
        assertEquals("tool_use", event.getEventType());
        assertEquals("tool_calls", event.getFinishReason());
        assertFalse(event.isDelta());
        assertEquals(1, event.getBlocks().size());
    }

    @Test
    void testStreamEventImplementsMessage() {
        StreamEvent event = StreamEvent.messageStart();

        assertTrue(event instanceof Message);
        assertEquals("assistant", event.getRole());
    }

    @Test
    void testStreamEventTextDeltaWithNonTextBlock() {
        StreamEvent event = StreamEvent.contentBlockDelta(0, new ToolUseBlock("id", "fn", null));

        assertEquals("", event.getTextDelta());
    }

    @Test
    void testStreamEventToString() {
        StreamEvent event = StreamEvent.messageStart();
        String str = event.toString();

        assertTrue(str.contains("StreamEvent"));
        assertTrue(str.contains("message_start"));
    }

    @Test
    void testStreamEventSequence() {
        StreamEvent start = StreamEvent.messageStart();
        StreamEvent blockStart = StreamEvent.contentBlockStart(0);
        StreamEvent delta1 = StreamEvent.contentBlockDelta(0, new TextBlock("Hello "));
        StreamEvent delta2 = StreamEvent.contentBlockDelta(0, new TextBlock("World"));
        StreamEvent blockStop = StreamEvent.contentBlockStop(0);
        StreamEvent msgDelta = StreamEvent.messageDelta("stop");
        StreamEvent stop = StreamEvent.messageStop();

        assertTrue(start.isMessageStart());
        assertTrue(blockStart.isContentBlockStart());
        assertTrue(delta1.isContentBlockDelta());
        assertTrue(delta2.isContentBlockDelta());
        assertTrue(blockStop.isContentBlockStop());
        assertTrue(msgDelta.isMessageDelta());
        assertEquals("stop", msgDelta.getFinishReason());
        assertTrue(stop.isMessageStop());
    }

    @Test
    void testStreamEventFinishReason() {
        StreamEvent event1 = StreamEvent.messageDelta("stop");
        StreamEvent event2 = StreamEvent.messageDelta("length");
        StreamEvent event3 = StreamEvent.messageDelta("tool_calls");

        assertEquals("stop", event1.getFinishReason());
        assertEquals("length", event2.getFinishReason());
        assertEquals("tool_calls", event3.getFinishReason());
    }

    @Test
    void testStreamEventBlocksImmutable() {
        StreamEvent event = StreamEvent.delta("Initial");
        List<github.ponyhuang.agentframework.types.block.Block> blocks = event.getBlocks();

        assertThrows(UnsupportedOperationException.class, () -> {
            blocks.add(new TextBlock("New"));
        });
    }
}

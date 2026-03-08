package github.ponyhuang.agentframework.types.block;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BlockTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ObjectMapper mapperIgnoreUnknown = new ObjectMapper();

    @Test
    void testTextBlockSerialization() throws Exception {
        TextBlock block = new TextBlock("Hello, world!");

        String json = mapper.writeValueAsString(block);

        assertTrue(json.contains("\"type\":\"text\""));
        assertTrue(json.contains("\"text\":\"Hello, world!\""));
    }

    @Test
    void testTextBlockDeserialization() throws Exception {
        String json = "{\"type\":\"text\",\"text\":\"Hello, world!\"}";

        TextBlock block = mapper.readValue(json, TextBlock.class);

        assertEquals("text", block.getType());
        assertEquals("Hello, world!", block.getText());
    }

    @Test
    void testTextBlockEquality() {
        TextBlock block1 = new TextBlock("Hello");
        TextBlock block2 = new TextBlock("Hello");
        TextBlock block3 = new TextBlock("World");

        assertEquals(block1, block2);
        assertNotEquals(block1, block3);
    }

    @Test
    void testTextBlockOf() {
        TextBlock block = TextBlock.of("Test content");

        assertEquals("text", block.getType());
        assertEquals("Test content", block.getText());
    }

    @Test
    void testThinkingBlockSerialization() throws Exception {
        ThinkingBlock block = new ThinkingBlock("Let me think about this...", "signature123");

        String json = mapper.writeValueAsString(block);

        assertTrue(json.contains("\"type\":\"thinking\""));
        assertTrue(json.contains("\"thinking\":\"Let me think about this...\""));
        assertTrue(json.contains("\"signature\":\"signature123\""));
    }

    @Test
    void testThinkingBlockDeserialization() throws Exception {
        String json = "{\"type\":\"thinking\",\"thinking\":\"Thinking process\",\"signature\":\"sig123\"}";

        ThinkingBlock block = mapper.readValue(json, ThinkingBlock.class);

        assertEquals("thinking", block.getType());
        assertEquals("Thinking process", block.getThinking());
        assertEquals("sig123", block.getSignature());
    }

    @Test
    void testThinkingBlockOf() {
        ThinkingBlock block = ThinkingBlock.of("My thoughts", "my_signature");

        assertEquals("thinking", block.getType());
        assertEquals("My thoughts", block.getThinking());
        assertEquals("my_signature", block.getSignature());
    }

    @Test
    void testToolUseBlockSerialization() throws Exception {
        ToolUseBlock block = ToolUseBlock.of("call_123", "get_weather", Map.of("location", "Paris"));

        String json = mapper.writeValueAsString(block);

        assertTrue(json.contains("\"type\":\"tool_use\""));
        assertTrue(json.contains("\"id\":\"call_123\""));
        assertTrue(json.contains("\"name\":\"get_weather\""));
        assertTrue(json.contains("\"location\":\"Paris\""));
    }

    @Test
    void testToolUseBlockDeserialization() throws Exception {
        String json = "{\"type\":\"tool_use\",\"id\":\"call_abc\",\"name\":\"my_function\",\"input\":{\"arg1\":\"value1\"}}";

        ToolUseBlock block = mapper.readValue(json, ToolUseBlock.class);

        assertEquals("tool_use", block.getType());
        assertEquals("call_abc", block.getId());
        assertEquals("my_function", block.getName());
        assertEquals("value1", block.getInput().get("arg1"));
    }

    @Test
    void testToolUseBlockOf() {
        Map<String, Object> input = Map.of("param1", "value1", "param2", 42);
        ToolUseBlock block = ToolUseBlock.of("tool_id_1", "myTool", input);

        assertEquals("tool_use", block.getType());
        assertEquals("tool_id_1", block.getId());
        assertEquals("myTool", block.getName());
        assertEquals("value1", block.getInput().get("param1"));
    }

    @Test
    void testToolResultBlockSerialization() throws Exception {
        ToolResultBlock block = ToolResultBlock.of("call_123", "The weather is sunny", false);

        String json = mapper.writeValueAsString(block);

        assertTrue(json.contains("\"type\":\"tool_result\""));
        assertTrue(json.contains("\"tool_use_id\":\"call_123\""));
        assertTrue(json.contains("\"content\":\"The weather is sunny\""));
    }

    @Test
    void testToolResultBlockDeserialization() throws Exception {
        String json = "{\"type\":\"tool_result\",\"tool_use_id\":\"call_xyz\",\"content\":\"Result data\",\"is_error\":false}";

        ToolResultBlock block = mapperIgnoreUnknown.readValue(json, ToolResultBlock.class);

        assertEquals("tool_result", block.getType());
        assertEquals("call_xyz", block.getToolUseId());
        assertEquals("Result data", block.getContent());
        assertEquals(false, block.getIsError());
    }

    @Test
    void testToolResultBlockWithError() {
        ToolResultBlock block = ToolResultBlock.of("call_error", "Error occurred", true);

        assertEquals("tool_result", block.getType());
        assertEquals("call_error", block.getToolUseId());
        assertEquals("Error occurred", block.getContent());
        assertEquals(true, block.getIsError());
    }

    @Test
    void testBlockPolymorphicDeserialization() throws Exception {
        String textJson = "{\"type\":\"text\",\"text\":\"Hello\"}";
        String thinkingJson = "{\"type\":\"thinking\",\"thinking\":\"Thinking...\"}";
        String toolUseJson = "{\"type\":\"tool_use\",\"id\":\"id1\",\"name\":\"fn\",\"input\":{}}";
        String toolResultJson = "{\"type\":\"tool_result\",\"tool_use_id\":\"id1\",\"content\":\"result\"}";

        Block textBlock = mapperIgnoreUnknown.readValue(textJson, Block.class);
        Block thinkingBlock = mapperIgnoreUnknown.readValue(thinkingJson, Block.class);
        Block toolUseBlock = mapperIgnoreUnknown.readValue(toolUseJson, Block.class);
        Block toolResultBlock = mapperIgnoreUnknown.readValue(toolResultJson, Block.class);

        assertTrue(textBlock instanceof TextBlock);
        assertTrue(thinkingBlock instanceof ThinkingBlock);
        assertTrue(toolUseBlock instanceof ToolUseBlock);
        assertTrue(toolResultBlock instanceof ToolResultBlock);

        assertEquals("Hello", ((TextBlock) textBlock).getText());
        assertEquals("Thinking...", ((ThinkingBlock) thinkingBlock).getThinking());
        assertEquals("fn", ((ToolUseBlock) toolUseBlock).getName());
        assertEquals("result", ((ToolResultBlock) toolResultBlock).getContent());
    }

    @Test
    void testTextBlockEmptyConstructor() {
        TextBlock block = new TextBlock();

        assertEquals("text", block.getType());
        assertEquals("", block.getText());
    }

    @Test
    void testThinkingBlockEmptyConstructor() {
        ThinkingBlock block = new ThinkingBlock();

        assertEquals("thinking", block.getType());
        assertEquals("", block.getThinking());
        assertEquals("", block.getSignature());
    }

    @Test
    void testToolUseBlockEmptyConstructor() {
        ToolUseBlock block = new ToolUseBlock();

        assertEquals("tool_use", block.getType());
        assertEquals("", block.getId());
        assertEquals("", block.getName());
        assertNull(block.getInput());
    }

    @Test
    void testToolResultBlockEmptyConstructor() {
        ToolResultBlock block = new ToolResultBlock();

        assertEquals("tool_result", block.getType());
        assertEquals("", block.getToolUseId());
        assertEquals("", block.getContent());
        assertEquals(false, block.getIsError());
    }
}

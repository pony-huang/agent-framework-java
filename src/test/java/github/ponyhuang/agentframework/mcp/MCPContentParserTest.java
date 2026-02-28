package github.ponyhuang.agentframework.mcp;

import github.ponyhuang.agentframework.types.Content;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MCPContentParser class.
 */
class MCPContentParserTest {

    /**
     * Test parsing text content.
     */
    @Test
    void testParseTextContent() {
        McpSchema.TextContent textContent = new McpSchema.TextContent("Hello, World!");
        Content result = MCPContentParser.parse(textContent);

        assertNotNull(result);
        assertEquals(Content.ContentType.TEXT, result.getType());
        assertEquals("Hello, World!", result.getText());
    }

    /**
     * Test parsing null content.
     */
    @Test
    void testParseNullContent() {
        Content result = MCPContentParser.parse((McpSchema.Content) null);
        assertNull(result);
    }

    /**
     * Test converting content list to text.
     */
    @Test
    void testToTextFromContentList() {
        List<McpSchema.Content> contents = List.of(
                new McpSchema.TextContent("Hello"),
                new McpSchema.TextContent("World")
        );

        String result = MCPContentParser.toText(contents);
        assertEquals("Hello\nWorld", result);
    }

    /**
     * Test toText with null list.
     */
    @Test
    void testToTextWithNullList() {
        String result = MCPContentParser.toText((List<McpSchema.Content>) null);
        assertEquals("", result);
    }

    /**
     * Test toText with empty list.
     */
    @Test
    void testToTextWithEmptyList() {
        String result = MCPContentParser.toText(List.of());
        assertEquals("", result);
    }

    /**
     * Test parseAll with null.
     */
    @Test
    void testParseAllWithNull() {
        List<Content> result = MCPContentParser.parseAll(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test parseAll with empty list.
     */
    @Test
    void testParseAllWithEmptyList() {
        List<Content> result = MCPContentParser.parseAll(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Test parseAll with multiple contents.
     */
    @Test
    void testParseAllWithMultipleContents() {
        List<McpSchema.Content> contents = List.of(
                new McpSchema.TextContent("Hello"),
                new McpSchema.TextContent("World")
        );

        List<Content> result = MCPContentParser.parseAll(contents);
        assertEquals(2, result.size());
        assertEquals("Hello", result.get(0).getText());
        assertEquals("World", result.get(1).getText());
    }

    /**
     * Test toText with null content.
     */
    @Test
    void testToTextWithNullContent() {
        String result = MCPContentParser.toText((McpSchema.Content) null);
        assertNull(result);
    }

    /**
     * Test toText with TextContent.
     */
    @Test
    void testToTextWithTextContent() {
        McpSchema.TextContent textContent = new McpSchema.TextContent("Hello");
        String result = MCPContentParser.toText(textContent);
        assertEquals("Hello", result);
    }
}

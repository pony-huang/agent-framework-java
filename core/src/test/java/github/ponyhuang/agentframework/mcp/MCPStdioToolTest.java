package github.ponyhuang.agentframework.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MCPStdioTool class.
 */
class MCPStdioToolTest {

    /**
     * Test MCPStdioTool builder creates tool with correct configuration.
     */
    @Test
    void testBuilderCreatesToolWithConfiguration() {
        MCPStdioTool tool = MCPStdioTool.builder()
                .name("test-server")
                .description("Test MCP server")
                .command("npx")
                .args(List.of("-y", "some-package"))
                .env(Map.of("NODE_ENV", "test"))
                .build();

        assertEquals("test-server", tool.getName());
        assertEquals("Test MCP server", tool.getDescription());
        assertEquals("npx", tool.getCommand());
        assertEquals(List.of("-y", "some-package"), tool.getArgs());
        assertEquals(Map.of("NODE_ENV", "test"), tool.getEnv());
    }

    /**
     * Test MCPStdioTool builder requires name.
     */
    @Test
    void testBuilderRequiresName() {
        assertThrows(IllegalArgumentException.class, () ->
            MCPStdioTool.builder()
                    .command("npx")
                    .build()
        );
    }

    /**
     * Test MCPStdioTool builder requires command.
     */
    @Test
    void testBuilderRequiresCommand() {
        assertThrows(IllegalArgumentException.class, () ->
            MCPStdioTool.builder()
                    .name("test")
                    .build()
        );
    }

    /**
     * Test MCPStdioTool defaults.
     */
    @Test
    void testBuilderDefaults() {
        MCPStdioTool tool = MCPStdioTool.builder()
                .name("test-server")
                .command("node")
                .build();

        assertFalse(tool.isConnected());
        assertNull(tool.getArgs());
        assertNull(tool.getEnv());
    }
}

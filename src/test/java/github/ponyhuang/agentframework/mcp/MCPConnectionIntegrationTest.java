package github.ponyhuang.agentframework.mcp;

import github.ponyhuang.agentframework.tools.FunctionTool;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MCP connections.
 * These tests require a running MCP server to execute fully.
 */
class MCPConnectionIntegrationTest {

    /**
     * Test MCPStdioTool connection lifecycle.
     * Requires npx and network access to download the MCP server package.
     */
    @Test
    @Disabled("Requires npx and network access to download MCP server package")
    void testStdioToolConnectionLifecycle() throws Exception {
        MCPStdioTool tool = MCPStdioTool.builder()
                .name("test-stdio")
                .description("Test stdio MCP tool")
                .command("npx")
                .args(List.of("-y", "@modelcontextprotocol/server-echo"))
                .loadTools(true)
                .loadPrompts(false)
                .build();

        try {
            // Test connection
            tool.connect();
            assertTrue(tool.isConnected(), "Tool should be connected");

            // Test tool loading
            List<FunctionTool> functions = tool.getFunctions();
            assertNotNull(functions, "Functions should not be null");

        } finally {
            tool.close();
            assertFalse(tool.isConnected(), "Tool should be disconnected");
        }
    }

    /**
     * Test MCPStdioTool tool invocation.
     * Requires a working MCP echo server.
     */
    @Test
    @Disabled("Requires npx and network access to download MCP server package")
    void testStdioToolCallTool() throws Exception {
        MCPStdioTool tool = MCPStdioTool.builder()
                .name("echo-server")
                .command("npx")
                .args(List.of("-y", "@modelcontextprotocol/server-echo"))
                .loadTools(true)
                .build();

        try {
            tool.connect();
            assertTrue(tool.isConnected());

            // Call a tool if available
            List<FunctionTool> functions = tool.getFunctions();
            if (!functions.isEmpty()) {
                FunctionTool function = functions.get(0);
                Object result = function.invoke(new HashMap<>());
                assertNotNull(result, "Tool result should not be null");
            }
        } finally {
            tool.close();
        }
    }

    /**
     * Test MCP tool auto-reconnect on connection loss.
     */
    @Test
    @Disabled("Requires network access and long-running test")
    void testToolReconnect() throws Exception {
        MCPStdioTool tool = MCPStdioTool.builder()
                .name("test-reconnect")
                .command("npx")
                .args(List.of("-y", "@modelcontextprotocol/server-echo"))
                .loadTools(true)
                .build();

        try {
            tool.connect();
            assertTrue(tool.isConnected());

            // Get functions before reconnect
            List<FunctionTool> functionsBefore = tool.getFunctions();

            // Simulate reconnect by calling close and connect
            tool.close();
            assertFalse(tool.isConnected());

            tool.connect();
            assertTrue(tool.isConnected());

            // Verify functions are still available
            List<FunctionTool> functionsAfter = tool.getFunctions();
            assertEquals(functionsBefore.size(), functionsAfter.size());
        } finally {
            tool.close();
        }
    }

    /**
     * Test HTTP MCP tool configuration without actual connection.
     */
    @Test
    void testHttpToolConfiguration() {
        MCPStreamableHTTPTool tool = MCPStreamableHTTPTool.builder()
                .name("http-test")
                .url("https://example.com/mcp")
                .loadTools(true)
                .build();

        assertEquals("https://example.com/mcp", tool.getUrl());
        assertFalse(tool.isConnected());
        assertNotNull(tool.getFunctions());
    }

    /**
     * Test approval mode configuration.
     */
    @Test
    void testApprovalModeConfiguration() {
        // Test ALWAYS_REQUIRE mode
        MCPStdioTool alwaysRequireTool = MCPStdioTool.builder()
                .name("always-require")
                .command("echo")
                .approvalMode(MCPTool.ApprovalMode.ALWAYS_REQUIRE)
                .build();
        assertNotNull(alwaysRequireTool);

        // Test NEVER_REQUIRE mode
        MCPStdioTool neverRequireTool = MCPStdioTool.builder()
                .name("never-require")
                .command("echo")
                .approvalMode(MCPTool.ApprovalMode.NEVER_REQUIRE)
                .build();
        assertNotNull(neverRequireTool);

        // Test SPECIFIC mode
        MCPStdioTool specificTool = MCPStdioTool.builder()
                .name("specific")
                .command("echo")
                .approvalMode(MCPTool.ApprovalMode.SPECIFIC)
                .approvalTools(List.of("tool1", "tool2"))
                .build();
        assertNotNull(specificTool);
    }

    /**
     * Test tool name normalization.
     */
    @Test
    void testToolNameNormalization() {
        MCPStdioTool tool = MCPStdioTool.builder()
                .name("test-tool")
                .command("echo")
                .allowedTools(List.of("tool with spaces", "tool-with-dashes"))
                .build();

        // Verify the tool was created (normalization happens internally)
        assertNotNull(tool);
    }

    /**
     * Test multiple MCP tools with same configuration.
     */
    @Test
    void testMultipleMcpTools() {
        MCPStdioTool tool1 = MCPStdioTool.builder()
                .name("server-1")
                .command("echo")
                .description("First server")
                .build();

        MCPStdioTool tool2 = MCPStdioTool.builder()
                .name("server-2")
                .command("echo")
                .description("Second server")
                .build();

        assertNotEquals(tool1.getName(), tool2.getName());
        assertNotEquals(tool1.getDescription(), tool2.getDescription());
    }
}

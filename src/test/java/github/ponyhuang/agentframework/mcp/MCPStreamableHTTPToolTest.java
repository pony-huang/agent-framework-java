package github.ponyhuang.agentframework.mcp;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MCPStreamableHTTPTool class.
 */
class MCPStreamableHTTPToolTest {

    /**
     * Test MCPStreamableHTTPTool builder creates tool with URL.
     */
    @Test
    void testBuilderCreatesToolWithUrl() {
        MCPStreamableHTTPTool tool = MCPStreamableHTTPTool.builder()
                .name("http-server")
                .url("https://api.example.com/mcp")
                .build();

        assertEquals("http-server", tool.getName());
        assertEquals("https://api.example.com/mcp", tool.getUrl());
    }

    /**
     * Test MCPStreamableHTTPTool builder requires name.
     */
    @Test
    void testBuilderRequiresName() {
        assertThrows(IllegalArgumentException.class, () ->
            MCPStreamableHTTPTool.builder()
                    .url("https://api.example.com/mcp")
                    .build()
        );
    }

    /**
     * Test MCPStreamableHTTPTool builder requires URL.
     */
    @Test
    void testBuilderRequiresUrl() {
        assertThrows(IllegalArgumentException.class, () ->
            MCPStreamableHTTPTool.builder()
                    .name("test")
                    .build()
        );
    }

    /**
     * Test MCPStreamableHTTPTool with custom HTTP client.
     */
    @Test
    void testBuilderWithCustomHttpClient() {
        HttpClient customClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        MCPStreamableHTTPTool tool = MCPStreamableHTTPTool.builder()
                .name("http-server")
                .url("https://api.example.com/mcp")
                .httpClient(customClient)
                .build();

        assertNotNull(tool.getUrl());
    }

    /**
     * Test MCPStreamableHTTPTool defaults.
     */
    @Test
    void testBuilderDefaults() {
        MCPStreamableHTTPTool tool = MCPStreamableHTTPTool.builder()
                .name("http-server")
                .url("https://api.example.com/mcp")
                .build();

        assertFalse(tool.isConnected());
    }
}

package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.mcp.MCPStdioTool;
import github.ponyhuang.agentframework.mcp.MCPStreamableHTTPTool;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;

import java.util.List;

/**
 * Examples demonstrating how to use MCP (Model Context Protocol) tools with agents.
 *
 * MCP allows agents to connect to external servers that provide tools, prompts, and resources.
 * This example shows three transport types: stdio, HTTP, and WebSocket.
 */
public class McpToolExample {

    public static void main(String[] args) {
        // This example requires MCP-enabled agents
        // Please ensure you have the necessary dependencies and MCP servers configured

        // Create a chat client (using OpenAI as example)
        ChatClient client = ClientExample.openAIChatClient();

        // Example 1: Stdio Transport (commented out as it requires a local MCP server)
        // example1StdioTransport(client);

        // Example 2: HTTP Transport
        // example2HttpTransport(client);

        System.out.println("MCP examples require MCP server configuration.");
        System.out.println("Uncomment the example methods to run them.");
    }

    static void example1StdioTransport(ChatClient client) {
        System.out.println("=== Example 1: Stdio Transport ===");

        // Create MCP stdio tool - similar to Python config:
        // {"command": "npx", "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/dir"]}
        MCPStdioTool mcpTool = MCPStdioTool.builder()
                .name("filesystem")
                .description("Filesystem server - read and write files")
                .command(findNpx())
                .args(List.of("-y", "@modelcontextprotocol/server-filesystem", System.getProperty("user.home")))
                .build();

        // Connect to MCP server
        mcpTool.connect();

        // Wait for connection
        waitForConnection(mcpTool);

        // Build agent with MCP tool
        Agent agent = AgentBuilder.builder()
                .name("filesystem-assistant")
                .instructions("You can use the available tools to help the user.")
                .client(client)
                .mcpTool(mcpTool)
                .build();

        // Run the agent
        ChatResponse response = agent.run(List.of(
                UserMessage.create("List all available tools you have access to.")
        ));

        System.out.println("Response: " + response.getMessage().getTextContent());

        // Clean up
        mcpTool.close();
    }

    static void example2HttpTransport(ChatClient client) {
        System.out.println("=== Example 2: HTTP Transport ===");

        MCPStreamableHTTPTool mcpTool = MCPStreamableHTTPTool.builder()
                .name("context7")
                .description("Context7 MCP Server - provides documentation and code search")
                .url("https://mcp.context7.com/mcp")
                .build();

        // Connect
        mcpTool.connect();

        // Wait for connection
        waitForHttpConnection(mcpTool);

        // Build agent with MCP tool
        Agent agent = AgentBuilder.builder()
                .name("context7-assistant")
                .instructions("You can search documentation using the available tools.")
                .client(client)
                .mcpTool(mcpTool)
                .build();

        // Run the agent
        ChatResponse response = agent.run(List.of(
                UserMessage.create("Search for information about Java streams API.")
        ));

        System.out.println("Response: " + response.getMessage().getTextContent());

        // Clean up
        mcpTool.close();
    }

    private static void waitForConnection(MCPStdioTool mcpTool) {
        System.out.println("Waiting for MCP server connection...");
        int retries = 10;
        while (!mcpTool.isConnected() && retries > 0) {
            try {
                Thread.sleep(1000);
                retries--;
            } catch (InterruptedException e) {
                break;
            }
        }
        if (!mcpTool.isConnected()) {
            throw new RuntimeException("Failed to connect to MCP server");
        }
        System.out.println("Connected!");
    }

    private static void waitForHttpConnection(MCPStreamableHTTPTool mcpTool) {
        System.out.println("Waiting for MCP server connection...");
        int retries = 10;
        while (!mcpTool.isConnected() && retries > 0) {
            try {
                Thread.sleep(1000);
                retries--;
            } catch (InterruptedException e) {
                break;
            }
        }
        if (!mcpTool.isConnected()) {
            throw new RuntimeException("Failed to connect to MCP server");
        }
        System.out.println("Connected!");
    }

    private static String findNpx() {
        // Try to find npx in PATH
        String[] paths = {"npx", "npx.cmd"};
        for (String path : paths) {
            try {
                Process p = Runtime.getRuntime().exec(path + " --version");
                if (p.waitFor() == 0) {
                    return path;
                }
            } catch (Exception e) {
                // Continue to next
            }
        }
        // Default to npx (Windows typically has npx.cmd)
        return "npx";
    }
}

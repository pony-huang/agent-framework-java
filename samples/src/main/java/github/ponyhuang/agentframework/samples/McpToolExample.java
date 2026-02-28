package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.mcp.MCPStdioTool;
import github.ponyhuang.agentframework.mcp.MCPStreamableHTTPTool;
import github.ponyhuang.agentframework.mcp.MCPTool.ApprovalMode;
import github.ponyhuang.agentframework.tools.FunctionTool;
import github.ponyhuang.agentframework.tools.ToolApprovalHandler;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Content;
import github.ponyhuang.agentframework.types.Message;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Examples demonstrating how to use MCP (Model Context Protocol) tools with agents.
 *
 * MCP allows agents to connect to external servers that provide tools, prompts, and resources.
 * This example shows three transport types: stdio, HTTP, and WebSocket.
 */
public class McpToolExample {

    // Default paths for npx on Windows - adjust if your Node.js is installed elsewhere
    private static final String[] NPX_PATHS = {
            "npx",  // Try PATH first
            System.getenv("LOCALAPPDATA") + "\\Microsoft\\WindowsApps\\npx.cmd",
            System.getenv("APPDATA") + "\\npm\\npx.cmd",
            "C:\\Program Files\\nodejs\\npx.cmd",
            "C:\\Program Files (x86)\\nodejs\\npx.cmd",
            // Common installation paths
            "D:\\env\\node-v24.12.0-win-x64\\npx.cmd",
            "D:\\Software\\node\\node_global\\npx.cmd"
    };

    /**
     * Find npx executable path.
     */
    private static String findNpx() {
        // First try PATH
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(";")) {
                File npx = new File(dir, "npx.cmd");
                if (npx.exists()) {
                    return npx.getAbsolutePath();
                }
                npx = new File(dir, "npx");
                if (npx.exists()) {
                    return npx.getAbsolutePath();
                }
            }
        }
        // Try common installation paths
        for (String npxPath : NPX_PATHS) {
            if (npxPath != null) {
                File npx = new File(npxPath);
                if (npx.exists()) {
                    return npx.getAbsolutePath();
                }
            }
        }
        return "npx"; // Fallback to just "npx" - will fail if not in PATH
    }

    public static void main(String[] args) {
        // Verify npx can be found
        String npxPath = findNpx();
        System.out.println("Using npx: " + npxPath);

        // Create a chat client
        ChatClient client = ClientExample.openAIChatClient();

        // Example 1: Using stdio transport (local MCP server)
//        example1StdioTransport(client);

        // Example 2: Using HTTP transport (remote MCP server)
//        example2HttpTransport(client);

        // Example 3: Using approval handler for tools that require approval
        example3ApprovalHandler(client);
    }

    /**
     * Example 1: Stdio Transport
     *
     * Connect to a local MCP server using stdio (standard input/output).
     * This is commonly used for CLI-based MCP servers.
     *
     * JSON configuration equivalent:
     * {
     *   "mcpServers": {
     *     "everything": {
     *       "command": "npx",
     *       "args": ["-y", "@modelcontextprotocol/server-everything"]
     *     }
     *   }
     * }
     */
    static void example1StdioTransport(ChatClient client) {
        System.out.println("=== Example 1: Stdio Transport ===");

        // Create MCP stdio tool - similar to Python config:
        // {"command": "npx", "args": ["-y", "@modelcontextprotocol/server-everything"]}
        MCPStdioTool mcpTool = MCPStdioTool.builder()
                .name("everything")
                .description("MCP Everything Server - provides tools, prompts, and resources")
                .command(findNpx())
                .args(List.of("-y", "@modelcontextprotocol/server-everything"))
                // Optional: environment variables
                // .env(Map.of("NODE_ENV", "production"))
                .build();

        // Build agent with MCP tool
        Agent agent = AgentBuilder.builder()
                .name("mcp-assistant")
                .instructions("You can use the available tools to help the user.")
                .client(client)
                .mcpTool(mcpTool)  // Add MCP tool
                .build();

        // Run the agent
        ChatResponse response = agent.run(List.of(
                Message.user("List all available tools you have access to.")
        ));

        System.out.println("Response: " + response.getMessage().getText());

        // Clean up
        mcpTool.close();
    }

    /**
     * Example 2: HTTP Transport
     *
     * Connect to a remote MCP server using HTTP/SSE (Server-Sent Events).
     *
     * JSON configuration equivalent:
     * {
     *   "mcpServers": {
     *     "context7": {
     *       "url": "https://mcp.context7.com/mcp"
     *     }
     *   }
     * }
     */
    static void example2HttpTransport(ChatClient client) {
        System.out.println("=== Example 2: HTTP Transport ===");

        // Create MCP HTTP tool - similar to Python config:
        // {"url": "https://mcp.context7.com/mcp"}
        MCPStreamableHTTPTool mcpTool = MCPStreamableHTTPTool.builder()
                .name("context7")
                .description("Context7 MCP Server - provides documentation and code search")
                .url("https://mcp.context7.com/mcp")
                // Optional: custom HTTP client with headers
                // .httpClient(HttpClient.newBuilder()
                //     .connectTimeout(Duration.ofSeconds(10))
                //     .build())
                .build();

        // Build agent with MCP tool
        Agent agent = AgentBuilder.builder()
                .name("context7-assistant")
                .instructions("You can search documentation using the available tools.")
                .client(client)
                .mcpTool(mcpTool)
                .build();

        // Run the agent
        ChatResponse response = agent.run(List.of(
                Message.user("Search for information about Java streams API.")
        ));

        System.out.println("Response: " + response.getMessage().getText());

        // Clean up
        mcpTool.close();
    }


    /**
     * Example 3: Using Approval Handler
     *
     * Shows how to use ToolExecutor with an approval handler for tools
     * that require approval before execution.
     */
    static void example3ApprovalHandler(ChatClient client) {
        System.out.println("=== Example 3: Approval Handler ===");

        // Create MCP tool with ALWAYS_REQUIRE approval mode
        MCPStdioTool mcpTool = MCPStdioTool.builder()
                .name("everything-server")
                .description("MCP server requiring approval for all tools")
                .command(findNpx())
                .args(List.of("-y", "@modelcontextprotocol/server-everything"))
                // All tools require approval
                .approvalMode(ApprovalMode.ALWAYS_REQUIRE)
                .build();

        // Connect to MCP server
        mcpTool.connect();

        // Create ToolExecutor and register MCP tools
        ToolExecutor executor = new ToolExecutor();
        for (FunctionTool func : mcpTool.getFunctions()) {
            executor.register(func);
        }

        // Debug: show all registered tools
        System.out.println("Total tools registered: " + executor.getToolCount());

        // Set up approval handler - this could prompt the user, check policies, etc.
        ToolApprovalHandler approvalHandler = (toolName, arguments) -> {
            System.out.println("\n=== Tool Approval Request ===");
            System.out.println("Tool: " + toolName);
            System.out.println("Arguments: " + arguments);
            System.out.print("Approve this tool call? (y/n): ");

            Scanner scanner = new Scanner(System.in);
            String response = scanner.nextLine().trim().toLowerCase();
            return "y".equals(response) || "yes".equals(response);
        };

        // Attach approval handler to executor
        executor.approvalHandler(approvalHandler);

        // Build agent with MCP tool (for tool schemas)
        Agent agent = AgentBuilder.builder()
                .name("approval-assistant")
                .instructions("You have access to tools that require approval.")
                .client(client)
                .mcpTool(mcpTool)
                .build();

        // Run agent loop with manual tool execution
        List<Message> messages = new ArrayList<>();
        // Use a prompt that actually requires tool execution, not just listing
        messages.add(Message.user("What is 5 + 7? Calculate it using the get-sum tool."));

        ChatResponse response = null;
        int maxRounds = 3;
        for (int i = 0; i < maxRounds; i++) {
            response = agent.run(messages);
            messages.add(response.getMessage());

            // Extract and execute tool calls
            List<Map<String, Object>> toolCalls = extractToolCalls(response.getMessage());
            if (toolCalls.isEmpty()) {
                break;
            }

            for (Map<String, Object> call : toolCalls) {
                String name = (String) call.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> args = (Map<String, Object>) call.get("arguments");
                String toolCallId = (String) call.get("id");

                // Debug: Check if tool exists and its approval status
                FunctionTool tool = executor.getTool(name);
                System.out.println("Executing tool: " + name + " found=" + (tool != null) + " requiresApproval=" + (tool != null ? tool.requiresApproval() : "N/A"));

                try {
                    // This will trigger the approval handler for tools requiring approval
                    Object result = executor.execute(name, args);
                    messages.add(Message.tool(toolCallId, name, result));
                    System.out.println("Tool result: " + result);
                } catch (SecurityException e) {
                    // Tool was rejected by approval handler
                    messages.add(Message.tool(toolCallId, name, "Tool execution rejected: " + e.getMessage()));
                    System.out.println("Tool rejected: " + e.getMessage());
                }
            }
        }

        System.out.println("\nFinal response: " + response.getMessage().getText());

        // Clean up
        mcpTool.close();
    }

    /**
     * Extract tool calls from an assistant message.
     */
    private static List<Map<String, Object>> extractToolCalls(Message message) {
        List<Map<String, Object>> calls = new ArrayList<>();
        if (message.getContents() == null) {
            return calls;
        }
        for (Content content : message.getContents()) {
            if (content.getType() == Content.ContentType.FUNCTION_CALL) {
                Map<String, Object> call = content.getFunctionCall();
                if (call != null && !call.isEmpty()) {
                    calls.add(call);
                }
            }
        }
        return calls;
    }
}

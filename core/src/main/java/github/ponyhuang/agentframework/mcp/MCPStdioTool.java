package github.ponyhuang.agentframework.mcp;

import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * MCP tool for connecting to stdio-based MCP servers.
 * This class connects to MCP servers that communicate via standard input/output,
 * typically used for local processes.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MCPStdioTool mcpTool = MCPStdioTool.builder()
 *     .name("filesystem")
 *     .command("npx")
 *     .args(List.of("-y", "@modelcontextprotocol/server-filesystem", "/tmp"))
 *     .description("File system operations")
 *     .build();
 *
 * mcpTool.connect();
 * Agent agent = Agent.builder()
 *     .client(client)
 *     .tools(mcpTool)
 *     .build();
 * }</pre>
 */
public class MCPStdioTool extends MCPTool {

    private static final Logger logger = LoggerFactory.getLogger(MCPStdioTool.class);

    private final String command;
    private final List<String> args;
    private final Map<String, String> env;

    protected MCPStdioTool(Builder builder) {
        super(builder);
        this.command = builder.command;
        this.args = builder.args;
        this.env = builder.env;
    }

    /**
     * Create a new Builder for MCPStdioTool.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the command used to start the MCP server.
     */
    public String getCommand() {
        return command;
    }

    /**
     * Get the arguments passed to the command.
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Get the environment variables.
     */
    public Map<String, String> getEnv() {
        return env;
    }

    @Override
    protected McpClientTransport createTransport() {
        ServerParameters.Builder builder = ServerParameters.builder(command);
        if (args != null && !args.isEmpty()) {
            builder.args(args);
        }
        if (env != null && !env.isEmpty()) {
            builder.env(env);
        }
        StdioClientTransport transport = new StdioClientTransport(builder.build(), McpJsonDefaults.getMapper());
        transport.setStdErrorHandler(line -> logger.warn("MCP stdio stderr ({}): {}", name, line));
        return transport;
    }

    /**
     * Builder for MCPStdioTool.
     */
    public static class Builder extends MCPTool.Builder<Builder> {
        private String command;
        private List<String> args;
        private Map<String, String> env;

        public MCPStdioTool build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name is required");
            }
            if (command == null || command.isEmpty()) {
                throw new IllegalArgumentException("Command is required");
            }
            return new MCPStdioTool(this);
        }

        /**
         * Set the command to run the MCP server.
         */
        public Builder command(String command) {
            this.command = command;
            return this;
        }

        /**
         * Set the arguments to pass to the command.
         */
        public Builder args(List<String> args) {
            this.args = args;
            return this;
        }

        /**
         * Set the environment variables.
         */
        public Builder env(Map<String, String> env) {
            this.env = env;
            return this;
        }
    }
}

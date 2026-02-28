package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.middleware.AgentMiddleware;
import github.ponyhuang.agentframework.mcp.MCPTool;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.ContextProvider;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.Message;
import github.ponyhuang.agentframework.tools.FunctionTool;
import github.ponyhuang.agentframework.sessions.InMemoryAgentSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for creating Agent instances.
 * Provides a fluent API for configuring agents.
 */
public class AgentBuilder {

    private String name = "assistant";
    private String instructions;
    private ChatClient client;
    private List<Map<String, Object>> tools = new ArrayList<>();
    private List<ContextProvider> contextProviders = new ArrayList<>();
    private List<AgentMiddleware> middlewares = new ArrayList<>();
    private Map<String, Object> defaultOptions = new HashMap<>();

    public static AgentBuilder builder() {
        return new AgentBuilder();
    }

    /**
     * Sets the agent name.
     *
     * @param name the agent name
     * @return this builder
     */
    public AgentBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the agent instructions (system prompt).
     *
     * @param instructions the instructions
     * @return this builder
     */
    public AgentBuilder instructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    /**
     * Sets the chat client.
     *
     * @param client the chat client
     * @return this builder
     */
    public AgentBuilder client(ChatClient client) {
        this.client = client;
        return this;
    }

    /**
     * Adds a tool to the agent.
     *
     * @param tool the tool definition
     * @return this builder
     */
    public AgentBuilder tool(Map<String, Object> tool) {
        if (tool != null) {
            this.tools.add(tool);
        }
        return this;
    }

    /**
     * Adds multiple tools to the agent.
     *
     * @param tools the tool definitions
     * @return this builder
     */
    public AgentBuilder tools(List<Map<String, Object>> tools) {
        if (tools != null) {
            this.tools.addAll(tools);
        }
        return this;
    }

    /**
     * Adds tools from an MCP tool to the agent.
     * This will connect to the MCP server and load available tools.
     *
     * @param mcpTool the MCP tool instance
     * @return this builder
     * @throws McpToolException if MCP connection fails
     */
    public AgentBuilder mcpTool(MCPTool mcpTool) {
        if (mcpTool != null) {
            // Connect to MCP server if not already connected
            if (!mcpTool.isConnected()) {
                try {
                    mcpTool.connect();
                } catch (Exception e) {
                    throw new McpToolException("Failed to connect to MCP server: " + mcpTool.getName(), e);
                }
            }
            // Add MCP tool functions as tool schemas
            for (FunctionTool func : mcpTool.getFunctions()) {
                this.tools.add(func.toSchema());
            }
        }
        return this;
    }

    /**
     * Adds multiple MCP tools to the agent.
     *
     * @param mcpTools the MCP tool instances
     * @return this builder
     */
    public AgentBuilder mcpTools(List<MCPTool> mcpTools) {
        if (mcpTools != null) {
            for (MCPTool mcpTool : mcpTools) {
                mcpTool(mcpTool);
            }
        }
        return this;
    }

    /**
     * Adds a context provider to the agent.
     *
     * @param provider the context provider
     * @return this builder
     */
    public AgentBuilder contextProvider(ContextProvider provider) {
        if (provider != null) {
            this.contextProviders.add(provider);
        }
        return this;
    }

    /**
     * Adds multiple context providers to the agent.
     *
     * @param providers the context providers
     * @return this builder
     */
    public AgentBuilder contextProviders(List<ContextProvider> providers) {
        if (providers != null) {
            this.contextProviders.addAll(providers);
        }
        return this;
    }

    /**
     * Adds a middleware to the agent.
     *
     * @param middleware the middleware
     * @return this builder
     */
    public AgentBuilder middleware(AgentMiddleware middleware) {
        if (middleware != null) {
            this.middlewares.add(middleware);
        }
        return this;
    }

    /**
     * Adds multiple middlewares to the agent.
     *
     * @param middlewares the middlewares
     * @return this builder
     */
    public AgentBuilder middlewares(List<AgentMiddleware> middlewares) {
        if (middlewares != null) {
            this.middlewares.addAll(middlewares);
        }
        return this;
    }

    /**
     * Sets default options for the agent.
     *
     * @param options the default options
     * @return this builder
     */
    public AgentBuilder defaultOptions(Map<String, Object> options) {
        if (options != null) {
            this.defaultOptions.putAll(options);
        }
        return this;
    }

    /**
     * Sets the default temperature.
     *
     * @param temperature the temperature
     * @return this builder
     */
    public AgentBuilder temperature(double temperature) {
        this.defaultOptions.put("temperature", temperature);
        return this;
    }

    /**
     * Sets the default max tokens.
     *
     * @param maxTokens the max tokens
     * @return this builder
     */
    public AgentBuilder maxTokens(int maxTokens) {
        this.defaultOptions.put("maxTokens", maxTokens);
        return this;
    }

    /**
     * Builds the agent.
     *
     * @return a new Agent instance
     * @throws IllegalStateException if required fields are not set
     */
    public Agent build() {
        if (client == null) {
            throw new IllegalStateException("ChatClient is required");
        }

        return new DefaultAgent(name, instructions, client, tools, contextProviders, middlewares, defaultOptions);
    }

    /**
     * Exception thrown when MCP tool operations fail.
     */
    public static class McpToolException extends RuntimeException {
        public McpToolException(String message) {
            super(message);
        }

        public McpToolException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Default implementation of Agent.
     */
    private static class DefaultAgent extends BaseAgent {

        public DefaultAgent(String name, String instructions, ChatClient client,
                           List<Map<String, Object>> tools, List<ContextProvider> contextProviders, List<AgentMiddleware> middlewares, Map<String, Object> defaultOptions) {
            super();
            this.name = name;
            this.instructions = instructions;
            this.client = client;
            this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
            this.contextProviders = contextProviders != null ? new ArrayList<>(contextProviders) : new ArrayList<>();
            this.middlewares = middlewares != null ? new ArrayList<>(middlewares) : new ArrayList<>();
            this.defaultOptions = defaultOptions != null ? new HashMap<>(defaultOptions) : new HashMap<>();
        }

        @Override
        protected AgentSession createSession(Map<String, Object> options) {
            // Default implementation - creates a simple in-memory session
            return new InMemoryAgentSession(this);
        }

        @Override
        protected ChatResponse doRun(List<Message> messages, Map<String, Object> options) {
            ChatCompleteParams params =
                    ChatCompleteParams.builder()
                            .messages(messages)
                            .model(client.getModel())
                            .tools(tools.isEmpty() ? null : tools)
                            .build();

            return client.chat(params);
        }

        @Override
        protected reactor.core.publisher.Flux<github.ponyhuang.agentframework.types.ChatResponse> doRunStream(
                List<Message> messages, Map<String, Object> options) {

            ChatCompleteParams params =
                    ChatCompleteParams.builder()
                            .messages(messages)
                            .model(client.getModel())
                            .tools(tools.isEmpty() ? null : tools)
                            .build();

            return client.chatStream(params);
        }
    }
}

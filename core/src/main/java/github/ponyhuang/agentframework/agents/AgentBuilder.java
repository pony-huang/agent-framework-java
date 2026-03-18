package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.hooks.Hook;
import github.ponyhuang.agentframework.hooks.HookEventBus;
import github.ponyhuang.agentframework.hooks.HookHandler;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.event.BaseEvent;
import github.ponyhuang.agentframework.hooks.event.HookEventType;
import github.ponyhuang.agentframework.mcp.MCPTool;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.tools.FunctionTool;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builder for creating Agent instances.
 * Provides a fluent API for configuring agents.
 */
public class AgentBuilder {

    private String name = "assistant";
    private String instructions;
    private ChatClient client;
    private List<Map<String, Object>> tools = new ArrayList<>();
    private Map<String, Object> defaultOptions = new HashMap<>();
    private ToolExecutor toolExecutor = new ToolExecutor();
    private HookEventBus hookEventBus = new HookEventBus();

    // New alignment features
    private Set<String> allowedTools = new HashSet<>();
    private Set<String> disallowedTools = new HashSet<>();
    private double maxBudgetUsd = 0.0;  // 0 means no limit
    private String fallbackModel;
    private PermissionMode permissionMode = PermissionMode.DEFAULT;
    private Map<String, AgentDefinition> agents = new HashMap<>();

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
     * Adds a tool to the agent by registering an instance with @Tool annotated methods.
     * This is a simplified alternative to using ToolExecutor directly.
     *
     * @param toolInstance the object containing @Tool annotated methods
     * @return this builder
     */
    public AgentBuilder tool(Object toolInstance) {
        if (toolInstance != null) {
            toolExecutor.registerAnnotated(toolInstance);
            // Add tool schema to the tools list
            for (Map<String, Object> schema : toolExecutor.getToolSchemas()) {
                // Avoid duplicates
                if (!tools.contains(schema)) {
                    this.tools.add(schema);
                }
            }
        }
        return this;
    }

    /**
     * Adds multiple tools to the agent by registering instances with @Tool annotated methods.
     *
     * @param toolInstances the objects containing @Tool annotated methods
     * @return this builder
     */
    public AgentBuilder tools(Object... toolInstances) {
        if (toolInstances != null) {
            for (Object toolInstance : toolInstances) {
                tool(toolInstance);
            }
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
     * Sets the tools that are auto-approved (always allowed).
     *
     * @param allowedTools the set of allowed tool names
     * @return this builder
     */
    public AgentBuilder allowedTools(Set<String> allowedTools) {
        if (allowedTools != null) {
            this.allowedTools = new HashSet<>(allowedTools);
        }
        return this;
    }

    /**
     * Sets the tools that are explicitly disallowed (always denied).
     *
     * @param disallowedTools the set of disallowed tool names
     * @return this builder
     */
    public AgentBuilder disallowedTools(Set<String> disallowedTools) {
        if (disallowedTools != null) {
            this.disallowedTools = new HashSet<>(disallowedTools);
        }
        return this;
    }

    /**
     * Sets the maximum budget in USD for the agent execution.
     * When the accumulated cost exceeds this limit, execution stops.
     *
     * @param maxBudgetUsd the maximum budget in USD (0 or negative means unlimited)
     * @return this builder
     */
    public AgentBuilder maxBudgetUsd(double maxBudgetUsd) {
        this.maxBudgetUsd = maxBudgetUsd > 0 ? maxBudgetUsd : 0.0;
        return this;
    }

    /**
     * Sets the fallback model to use when the primary model fails.
     *
     * @param fallbackModel the fallback model identifier
     * @return this builder
     */
    public AgentBuilder fallbackModel(String fallbackModel) {
        this.fallbackModel = fallbackModel;
        return this;
    }

    /**
     * Sets the permission mode for tool execution.
     *
     * @param permissionMode the permission mode
     * @return this builder
     */
    public AgentBuilder permissionMode(PermissionMode permissionMode) {
        this.permissionMode = permissionMode != null ? permissionMode : PermissionMode.DEFAULT;
        return this;
    }

    /**
     * Gets the allowed tools set.
     */
    public Set<String> getAllowedTools() {
        return allowedTools;
    }

    /**
     * Gets the disallowed tools set.
     */
    public Set<String> getDisallowedTools() {
        return disallowedTools;
    }

    /**
     * Gets the max budget in USD.
     */
    public double getMaxBudgetUsd() {
        return maxBudgetUsd;
    }

    /**
     * Gets the fallback model.
     */
    public String getFallbackModel() {
        return fallbackModel;
    }

    /**
     * Gets the permission mode.
     */
    public PermissionMode getPermissionMode() {
        return permissionMode;
    }

    /**
     * Adds a custom sub-agent definition.
     *
     * @param agent the agent definition
     * @return this builder
     */
    public AgentBuilder agent(AgentDefinition agent) {
        if (agent != null && agent.getName() != null) {
            this.agents.put(agent.getName(), agent);
        }
        return this;
    }

    /**
     * Adds multiple custom sub-agent definitions.
     *
     * @param agents map of agent name to definition
     * @return this builder
     */
    public AgentBuilder agents(Map<String, AgentDefinition> agents) {
        if (agents != null) {
            this.agents.putAll(agents);
        }
        return this;
    }

    /**
     * Gets the registered agents.
     */
    public Map<String, AgentDefinition> getAgents() {
        return agents;
    }

    /**
     * Adds a hook for specific event types.
     *
     * @param hook the hook implementation
     * @return this builder
     */
    public AgentBuilder hook(Hook hook) {
        hookEventBus.registerHook(hook);
        return this;
    }

    /**
     * Adds a hook for a specific event type with lambda.
     *
     * @param eventType the hook event type
     * @param handler the hook handler (lambda)
     * @return this builder
     */
    public AgentBuilder on(HookEventType eventType, java.util.function.Function<BaseEvent, HookResult> handler) {
        return on(eventType, handler, null);
    }

    /**
     * Adds a hook for a specific event type with lambda and matcher.
     *
     * @param eventType the hook event type
     * @param handler the hook handler (lambda)
     * @param matcher the regex matcher pattern
     * @return this builder
     */
    public AgentBuilder on(HookEventType eventType, java.util.function.Function<BaseEvent, HookResult> handler, String matcher) {
        hookEventBus.registerHook(eventType, handler, matcher);
        return this;
    }

    /**
     * Adds a hook handler for a specific event.
     *
     * @param eventType the hook event type
     * @param handler the hook handler
     * @return this builder
     */
    public AgentBuilder hook(HookEventType eventType, HookHandler handler) {
        hookEventBus.registerHook(eventType, handler);
        return this;
    }

    /**
     * Adds a hook using a lambda function.
     *
     * @param eventType the hook event type
     * @param handler the hook handler (lambda)
     * @return this builder
     */
    public AgentBuilder hook(HookEventType eventType, java.util.function.Function<BaseEvent, HookResult> handler) {
        return on(eventType, handler);
    }

    /**
     * Adds a hook using a lambda function with matcher.
     *
     * @param eventType the hook event type
     * @param handler the hook handler (lambda)
     * @param matcher the regex matcher pattern
     * @return this builder
     */
    public AgentBuilder hook(HookEventType eventType, java.util.function.Function<BaseEvent, HookResult> handler, String matcher) {
        return on(eventType, handler, matcher);
    }

    /**
     * Adds a hook handler for a specific event with a matcher.
     *
     * @param eventType the hook event type
     * @param handler the hook handler
     * @param matcher the regex matcher (e.g., "Bash" for tool name)
     * @return this builder
     */
    public AgentBuilder hook(HookEventType eventType, HookHandler handler, String matcher) {
        hookEventBus.registerHook(eventType, handler, matcher);
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

        return new DefaultAgent(name, instructions, client, tools, defaultOptions, hookEventBus, agents);
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
                           List<Map<String, Object>> tools,
                           Map<String, Object> defaultOptions,
                           HookEventBus hookEventBus,
                           Map<String, AgentDefinition> agents) {
            super();
            this.name = name;
            this.instructions = instructions;
            this.client = client;
            this.tools = tools != null ? new ArrayList<>(tools) : new ArrayList<>();
            this.defaultOptions = defaultOptions != null ? new HashMap<>(defaultOptions) : new HashMap<>();
            this.hookEventBus = hookEventBus;
            if (agents != null) {
                this.agents.putAll(agents);
            }
        }

        @Override
        protected Flux<Message> doRun(List<Message> messages, Map<String, Object> options) {
            ChatCompleteParams params =
                    ChatCompleteParams.builder()
                            .messages(messages)
                            .model(client.getModel())
                            .tools(tools.isEmpty() ? null : tools)
                            .build();
            return Flux.just(client.chat(params).getMessage());
        }
    }
}

package github.ponyhuang.agentframework.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Executes tools and manages tool registry.
 */
public class ToolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ToolExecutor.class);

    private final Map<String, FunctionTool> tools = new ConcurrentHashMap<>();
    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private ToolApprovalHandler approvalHandler;

    /**
     * Registers a tool.
     *
     * @param tool the tool to register
     * @return this executor for chaining
     */
    public ToolExecutor register(FunctionTool tool) {
        if (tool != null) {
            tools.put(tool.getName(), tool);
            LOG.info("Tool registered: {}", tool.getName());
        }
        return this;
    }

    /**
     * Registers multiple tools.
     *
     * @param tools the tools to register
     * @return this executor for chaining
     */
    public ToolExecutor registerAll(Collection<FunctionTool> tools) {
        if (tools != null) {
            for (FunctionTool tool : tools) {
                register(tool);
            }
        }
        return this;
    }

    /**
     * Registers a method as a tool.
     *
     * @param name     the tool name
     * @param method   the method
     * @param instance the instance to invoke on
     * @return this executor for chaining
     */
    public ToolExecutor registerMethod(String name, Method method, Object instance) {
        FunctionTool tool = FunctionTool.builder()
                .name(name)
                .method(method)
                .instance(instance)
                .build();
        return register(tool);
    }

    /**
     * Registers all public methods annotated with @Tool.
     *
     * @param instance the object with annotated methods
     * @return this executor for chaining
     */
    public ToolExecutor registerAnnotated(Object instance) {
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getMethods()) {
            Tool toolAnnotation = method.getAnnotation(Tool.class);
            if (toolAnnotation != null) {
                String name = toolAnnotation.name();
                if (name == null || name.isEmpty()) {
                    name = method.getName();
                }
                String description = toolAnnotation.description();
                boolean requiresApproval = toolAnnotation.requiresApproval();

                FunctionTool tool = FunctionTool.builder()
                        .name(name)
                        .description(description)
                        .method(method)
                        .instance(instance)
                        .requiresApproval(requiresApproval)
                        .build();

                register(tool);
            }
        }
        return this;
    }

    /**
     * Removes a tool.
     *
     * @param name the tool name
     * @return this executor for chaining
     */
    public ToolExecutor unregister(String name) {
        tools.remove(name);
        return this;
    }

    /**
     * Gets a tool by name.
     *
     * @param name the tool name
     * @return the tool, or null if not found
     */
    public FunctionTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * Checks if a tool exists.
     *
     * @param name the tool name
     * @return true if the tool exists
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * Gets all registered tools.
     *
     * @return list of tools
     */
    public List<FunctionTool> getTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * Gets tool schemas for LLM.
     *
     * @return list of tool schemas
     */
    public List<Map<String, Object>> getToolSchemas() {
        return tools.values().stream()
                .map(FunctionTool::toSchema)
                .toList();
    }

    /**
     * Sets the approval handler for tools that require approval.
     *
     * @param handler the approval handler
     * @return this executor for chaining
     */
    public ToolExecutor approvalHandler(ToolApprovalHandler handler) {
        this.approvalHandler = handler;
        return this;
    }

    /**
     * Executes a tool by name with arguments.
     *
     * @param name      the tool name
     * @param arguments the arguments
     * @return the result
     * @throws IllegalArgumentException if tool not found
     * @throws SecurityException if tool requires approval and is not approved
     */
    public Object execute(String name, Map<String, Object> arguments) {
        LOG.info("Executing tool: {}", name);
        FunctionTool tool = tools.get(name);
        if (tool == null) {
            LOG.error("Tool not found: {}", name);
            throw new IllegalArgumentException("Tool not found: " + name);
        }

        // Check if approval is required
        if (tool.requiresApproval()) {
            if (approvalHandler == null) {
                LOG.warn("Tool {} requires approval but no approval handler is set", name);
                throw new SecurityException("Tool '" + name + "' requires approval but no approval handler is configured");
            }
            boolean approved = approvalHandler.approve(name, arguments != null ? arguments : Map.of());
            if (!approved) {
                LOG.info("Tool execution rejected by approval handler: {}", name);
                throw new SecurityException("Tool execution was rejected for tool: " + name);
            }
            LOG.info("Tool {} approved by approval handler", name);
        }

        try {
            Object result = tool.invoke(arguments);
            LOG.info("Tool executed successfully: {}", name);
            return result;
        } catch (Exception e) {
            LOG.error("Tool execution failed: {}, error: {}", name, e.getMessage());
            throw e;
        }
    }

    /**
     * Executes multiple tool calls.
     *
     * @param calls the tool calls (Map with "name" and "arguments")
     * @return list of results in the same order
     */
    public List<Object> executeAll(List<Map<String, Object>> calls) {
        if (calls == null) {
            return Collections.emptyList();
        }
        return calls.stream()
                .map(call -> {
                    String name = (String) call.get("name");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> args = (Map<String, Object>) call.get("arguments");
                    return execute(name, args != null ? args : Collections.emptyMap());
                })
                .toList();
    }

    /**
     * Clears all registered tools.
     *
     * @return this executor for chaining
     */
    public ToolExecutor clear() {
        tools.clear();
        methodCache.clear();
        return this;
    }

    /**
     * Gets the number of registered tools.
     *
     * @return the tool count
     */
    public int getToolCount() {
        return tools.size();
    }
}

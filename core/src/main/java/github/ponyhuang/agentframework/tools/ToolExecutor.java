package github.ponyhuang.agentframework.tools;

import github.ponyhuang.agentframework.hooks.HookEventBus;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.events.PermissionRequestContext;
import github.ponyhuang.agentframework.hooks.events.PostToolUseContext;
import github.ponyhuang.agentframework.hooks.events.PostToolUseFailureContext;
import github.ponyhuang.agentframework.hooks.events.PreToolUseContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/**
 * Executes tools and manages tool registry.
 */
public class ToolExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ToolExecutor.class);

    private final Map<String, FunctionTool> tools = new ConcurrentHashMap<>();
    private final Map<String, Method> methodCache = new ConcurrentHashMap<>();
    private HookEventBus hookEventBus;

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

                FunctionTool tool = FunctionTool.builder()
                        .name(name)
                        .description(description)
                        .method(method)
                        .instance(instance)
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
     * Sets the hook event bus for tool execution hooks.
     *
     * @param hookEventBus the hook event bus
     * @return this executor for chaining
     */
    public ToolExecutor hookEventBus(HookEventBus hookEventBus) {
        this.hookEventBus = hookEventBus;
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

        Map<String, Object> args = arguments != null ? arguments : Map.of();
        String toolUseId = UUID.randomUUID().toString();

        // Fire PreToolUse hook
        if (hookEventBus != null) {
            PreToolUseContext preContext = new PreToolUseContext();
            preContext.setToolName(name);
            preContext.setToolInput(args);
            preContext.setToolUseId(toolUseId);
            preContext.setCwd(System.getProperty("user.dir"));
            preContext.setPermissionMode("default");

            HookResult preResult = hookEventBus.executePreToolUse(preContext);

            // If hook denies, throw exception
            if (!preResult.isAllow()) {
                LOG.info("Tool {} blocked by PreToolUse hook: {}", name, preResult.getReason());
                throw new SecurityException("Tool '" + name + "' blocked by hook: " + preResult.getReason());
            }

            // Apply updated input if modified by hook
            if (preResult.getHookSpecificOutput() != null) {
                Object updatedInput = preResult.getHookSpecificOutput().get("updatedInput");
                if (updatedInput instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> updated = (Map<String, Object>) updatedInput;
                    args = updated;
                }
            }
        }

        // Request permission via hook if hookEventBus is available
        if (hookEventBus != null) {
            PermissionRequestContext permissionContext = new PermissionRequestContext();
            permissionContext.setToolName(name);
            permissionContext.setToolInput(args);
            permissionContext.setCwd(System.getProperty("user.dir"));
            permissionContext.setPermissionMode("default");

            HookResult permissionResult = hookEventBus.executePermissionRequest(permissionContext);

            if (!permissionResult.isAllow()) {
                LOG.info("Tool {} blocked by PermissionRequest hook: {}", name, permissionResult.getReason());
                throw new SecurityException("Tool '" + name + "' blocked by permission hook: " + permissionResult.getReason());
            }
        }

        try {
            Object result = tool.invoke(args);
            LOG.info("Tool executed successfully: {}", name);

            // Fire PostToolUse hook
            if (hookEventBus != null) {
                PostToolUseContext postContext = new PostToolUseContext();
                postContext.setToolName(name);
                postContext.setToolInput(args);
                postContext.setToolResponse(convertResultToMap(result));
                postContext.setToolUseId(toolUseId);
                postContext.setCwd(System.getProperty("user.dir"));
                postContext.setPermissionMode("default");
                hookEventBus.executePostToolUse(postContext);
            }

            return result;
        } catch (Exception e) {
            LOG.error("Tool execution failed: {}, error: {}", name, e.getMessage());

            // Fire PostToolUseFailure hook
            if (hookEventBus != null) {
                PostToolUseFailureContext failureContext = new PostToolUseFailureContext();
                failureContext.setToolName(name);
                failureContext.setToolInput(args);
                failureContext.setToolUseId(toolUseId);
                failureContext.setError(e.getMessage());
                failureContext.setCwd(System.getProperty("user.dir"));
                failureContext.setPermissionMode("default");
                hookEventBus.executePostToolUseFailure(failureContext);
            }

            throw e;
        }
    }

    private Map<String, Object> convertResultToMap(Object result) {
        if (result == null) {
            return Map.of();
        }
        // Simple conversion - for complex objects would need more sophisticated handling
        Map<String, Object> map = new HashMap<>();
        map.put("result", result.toString());
        return map;
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

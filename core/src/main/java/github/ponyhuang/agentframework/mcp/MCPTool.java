package github.ponyhuang.agentframework.mcp;

import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.tools.FunctionTool;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.message.AssistantMessage;
import github.ponyhuang.agentframework.types.block.TextBlock;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpTransportException;
import io.modelcontextprotocol.spec.McpTransportSessionClosedException;
import io.modelcontextprotocol.spec.McpTransportSessionNotFoundException;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Base class for MCP tool implementations.
 * Provides connection management, tool loading, prompt loading, and communication with MCP servers.
 *
 * <p>This class provides a simplified interface for connecting to MCP servers.
 * The actual MCP client implementation is delegated to subclasses which handle
 * the specific transport (stdio, HTTP, WebSocket).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Using stdio transport
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
public abstract class MCPTool {

    private static final Logger logger = LoggerFactory.getLogger(MCPTool.class);

    /**
     * Approval mode for MCP tools.
     */
    public enum ApprovalMode {
        /** Always require approval before executing any tool */
        ALWAYS_REQUIRE,
        /** Never require approval for tools */
        NEVER_REQUIRE,
        /** Use specific tool approval configuration */
        SPECIFIC
    }

    protected final String name;
    protected final String description;
    protected final ApprovalMode approvalMode;
    protected final Set<String> allowedTools;
    protected final boolean loadTools;
    protected final boolean loadPrompts;
    protected final ChatClient chatClient;
    protected final Duration requestTimeout;
    protected final Set<String> approvalTools;
    protected final McpSamplingHandler samplingHandler;
    protected final McpLoggingHandler loggingHandler;

    protected boolean connected = false;
    protected final Map<String, FunctionTool> functions = new ConcurrentHashMap<>();
    protected final Map<String, FunctionTool> prompts = new ConcurrentHashMap<>();
    protected final Map<String, String> toolNameMap = new ConcurrentHashMap<>();
    protected final Map<String, String> promptNameMap = new ConcurrentHashMap<>();

    protected McpSyncClient client;
    protected McpClientTransport transport;

    protected MCPTool(Builder<?> builder) {
        this.name = builder.name;
        this.description = builder.description != null ? builder.description : "";
        this.approvalMode = builder.approvalMode;
        this.allowedTools = normalizeToolSet(builder.allowedTools);
        this.approvalTools = normalizeToolSet(builder.approvalTools);
        this.loadTools = builder.loadTools;
        this.loadPrompts = builder.loadPrompts;
        this.chatClient = builder.chatClient;
        this.requestTimeout = builder.requestTimeout;
        this.samplingHandler = builder.samplingHandler;
        this.loggingHandler = builder.loggingHandler;
    }

    /**
     * Connect to the MCP server.
     * Subclasses should implement the actual connection logic.
     */
    public void connect() {
        if (connected) {
            logger.debug("Already connected to MCP server: {}", name);
            return;
        }
        try {
            transport = createTransport();
            McpClient.SyncSpec spec = McpClient.sync(transport);
            if (requestTimeout != null) {
                spec.requestTimeout(requestTimeout).initializationTimeout(requestTimeout);
            }
            spec.clientInfo(new McpSchema.Implementation("agent-framework-java", "Agent Framework Java"));
            spec.transportContextProvider(this::buildTransportContext);
            configureCallbacks(spec);
            client = spec.build();
            initializeSession();
            if (loadTools) {
                loadTools();
            }
            if (loadPrompts) {
                try {
                    loadPrompts();
                } catch (Exception e) {
                    logger.warn("Failed to load prompts from MCP server {}: {}", name, e.getMessage());
                }
            }
            connected = true;
            logger.info("Connected to MCP server: {}", name);
        } catch (Exception e) {
            close();
            throw new McpException("Failed to connect to MCP server: " + name, e);
        }
    }

    /**
     * Connect synchronously to the MCP server.
     * Default implementation calls connect().
     */
    public void connectSync() {
        connect();
    }

    /**
     * Close the connection to the MCP server.
     * Subclasses should implement cleanup logic.
     */
    public void close() {
        connected = false;
        functions.clear();
        prompts.clear();
        toolNameMap.clear();
        promptNameMap.clear();
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                logger.warn("Failed to close MCP client for {}: {}", name, e.getMessage());
            } finally {
                client = null;
            }
        }
        transport = null;
        logger.info("Disconnected from MCP server: {}", name);
    }

    /**
     * Call a tool on the MCP server.
     * Subclasses should implement the actual tool invocation.
     *
     * @param toolName the name of the tool to call
     * @param arguments the arguments to pass to the tool
     * @return the result as a string
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        ensureConnected();
        String resolvedName = resolveToolName(toolName);
        Supplier<String> action = () -> {
            McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
                    .name(resolvedName)
                    .arguments(arguments != null ? arguments : Map.of())
                    .build();
            McpSchema.CallToolResult result = client.callTool(request);
            return renderCallToolResult(result);
        };
        return withReconnect(action);
    }

    /**
     * Get a prompt from the MCP server.
     * Subclasses should implement the actual prompt retrieval.
     *
     * @param promptName the name of the prompt to retrieve
     * @param arguments the arguments to pass to the prompt
     * @return the prompt result as a string
     */
    public String getPrompt(String promptName, Map<String, Object> arguments) {
        ensureConnected();
        String resolvedName = resolvePromptName(promptName);
        Supplier<String> action = () -> {
            McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest(
                    resolvedName,
                    arguments != null ? arguments : Map.of()
            );
            McpSchema.GetPromptResult result = client.getPrompt(request);
            return renderPromptResult(result);
        };
        return withReconnect(action);
    }

    /**
     * Get the list of functions provided by this MCP tool.
     */
    public List<FunctionTool> getFunctions() {
        if (allowedTools == null) {
            return new ArrayList<>(functions.values());
        }
        return functions.values().stream()
                .filter(f -> allowedTools.contains(f.getName()))
                .toList();
    }

    /**
     * Get the list of prompts provided by this MCP tool.
     */
    public List<FunctionTool> getPrompts() {
        if (allowedTools == null) {
            return new ArrayList<>(prompts.values());
        }
        return prompts.values().stream()
                .filter(f -> allowedTools.contains(f.getName()))
                .toList();
    }

    /**
     * Check if connected to MCP server.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Get the name of this MCP tool.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description of this MCP tool.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Normalize MCP name to allowed identifier pattern.
     */
    protected String normalizeName(String name) {
        return name.replaceAll("[^A-Za-z0-9_.-]", "-");
    }

    /**
     * Determine approval mode for a specific tool.
     */
    protected boolean determineApprovalMode(String toolName) {
        if (approvalMode == ApprovalMode.ALWAYS_REQUIRE) {
            return true;
        }
        if (approvalMode == ApprovalMode.NEVER_REQUIRE) {
            return false;
        }
        if (approvalTools == null || approvalTools.isEmpty()) {
            return false;
        }
        return approvalTools.contains(normalizeName(toolName));
    }

    /**
     * Create the MCP transport for this tool.
     */
    protected abstract McpClientTransport createTransport();

    /**
     * Initialize MCP session.
     */
    protected void initializeSession() {
        if (client == null) {
            throw new McpException("MCP client is not initialized");
        }
        if (!client.isInitialized()) {
            client.initialize();
        }
    }

    protected void configureCallbacks(McpClient.SyncSpec spec) {
        if (spec == null) {
            return;
        }
        boolean hasSampling = false;
        if (samplingHandler != null) {
            spec.sampling(samplingHandler::onSample);
            hasSampling = true;
        } else if (chatClient != null) {
            spec.sampling(this::handleSamplingRequest);
            hasSampling = true;
        }
        if (hasSampling) {
            spec.capabilities(McpSchema.ClientCapabilities.builder().sampling().build());
        }
        if (loggingHandler != null) {
            spec.loggingConsumer(loggingHandler::onLog);
        } else {
            spec.loggingConsumer(this::handleLoggingNotification);
        }
        spec.progressConsumer(this::handleProgressNotification);
    }

    protected void loadTools() {
        functions.clear();
        toolNameMap.clear();
        String cursor = null;
        do {
            McpSchema.ListToolsResult result = cursor == null ? client.listTools() : client.listTools(cursor);
            if (result != null && result.tools() != null) {
                for (McpSchema.Tool tool : result.tools()) {
                    registerTool(tool);
                }
            }
            cursor = result != null ? result.nextCursor() : null;
        } while (cursor != null && !cursor.isBlank());
    }

    protected void loadPrompts() {
        prompts.clear();
        promptNameMap.clear();
        String cursor = null;
        do {
            McpSchema.ListPromptsResult result = cursor == null ? client.listPrompts() : client.listPrompts(cursor);
            if (result != null && result.prompts() != null) {
                for (McpSchema.Prompt prompt : result.prompts()) {
                    registerPrompt(prompt);
                }
            }
            cursor = result != null ? result.nextCursor() : null;
        } while (cursor != null && !cursor.isBlank());
    }

    protected void registerTool(McpSchema.Tool tool) {
        if (tool == null || tool.name() == null || tool.name().isBlank()) {
            return;
        }
        String normalized = normalizeName(tool.name());
        if (allowedTools != null && !allowedTools.contains(normalized)) {
            return;
        }
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", normalized);
        schema.put("description", tool.description());
        schema.put("parameters", toJsonSchemaMap(tool.inputSchema()));

        FunctionTool functionTool = FunctionTool.builder()
                .name(normalized)
                .description(tool.description() != null ? tool.description() : "")
                .schema(schema)
                .invoker(args -> callTool(tool.name(), args))
                .build();
        functions.put(normalized, functionTool);
        toolNameMap.put(normalized, tool.name());
    }

    protected void registerPrompt(McpSchema.Prompt prompt) {
        if (prompt == null || prompt.name() == null || prompt.name().isBlank()) {
            return;
        }
        String normalized = normalizeName(prompt.name());
        if (allowedTools != null && !allowedTools.contains(normalized)) {
            return;
        }
        Map<String, Object> schema = new HashMap<>();
        schema.put("name", normalized);
        String description = prompt.description() != null ? prompt.description() : prompt.title();
        if (description != null) {
            schema.put("description", description);
        }
        schema.put("parameters", promptArgumentsToSchema(prompt.arguments()));

        FunctionTool functionTool = FunctionTool.builder()
                .name(normalized)
                .description(description != null ? description : "")
                .schema(schema)
                .invoker(args -> getPrompt(prompt.name(), args))
                .build();
        prompts.put(normalized, functionTool);
        promptNameMap.put(normalized, prompt.name());
    }

    protected Map<String, Object> toJsonSchemaMap(McpSchema.JsonSchema schema) {
        if (schema == null) {
            return Map.of("type", "object");
        }
        Map<String, Object> result = new HashMap<>();
        if (schema.type() != null) {
            result.put("type", schema.type());
        }
        if (schema.properties() != null) {
            result.put("properties", schema.properties());
        }
        if (schema.required() != null) {
            result.put("required", schema.required());
        }
        if (schema.additionalProperties() != null) {
            result.put("additionalProperties", schema.additionalProperties());
        }
        if (schema.defs() != null) {
            result.put("$defs", schema.defs());
        }
        if (schema.definitions() != null) {
            result.put("definitions", schema.definitions());
        }
        return result;
    }

    protected Map<String, Object> promptArgumentsToSchema(List<McpSchema.PromptArgument> arguments) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();
        if (arguments != null) {
            for (McpSchema.PromptArgument arg : arguments) {
                if (arg == null || arg.name() == null) {
                    continue;
                }
                Map<String, Object> param = new HashMap<>();
                param.put("type", "string");
                if (arg.description() != null) {
                    param.put("description", arg.description());
                } else if (arg.title() != null) {
                    param.put("description", arg.title());
                }
                properties.put(arg.name(), param);
                if (Boolean.TRUE.equals(arg.required())) {
                    required.add(arg.name());
                }
            }
        }
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    protected String renderCallToolResult(McpSchema.CallToolResult result) {
        if (result == null) {
            return "";
        }
        if (result.structuredContent() != null) {
            try {
                return McpJsonDefaults.getMapper().writeValueAsString(result.structuredContent());
            } catch (Exception e) {
                return String.valueOf(result.structuredContent());
            }
        }
        if (result.content() == null || result.content().isEmpty()) {
            return "";
        }
        return MCPContentParser.toText(result.content());
    }

    protected String renderPromptResult(McpSchema.GetPromptResult result) {
        if (result == null || result.messages() == null) {
            return "";
        }
        return result.messages().stream()
                .map(message -> MCPContentParser.toText(message.content()))
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }

    protected void ensureConnected() {
        if (!connected) {
            connect();
        }
    }

    protected String resolveToolName(String toolName) {
        if (toolName == null) {
            return null;
        }
        String normalized = normalizeName(toolName);
        return toolNameMap.getOrDefault(normalized, toolName);
    }

    protected String resolvePromptName(String promptName) {
        if (promptName == null) {
            return null;
        }
        String normalized = normalizeName(promptName);
        return promptNameMap.getOrDefault(normalized, promptName);
    }

    protected <T> T withReconnect(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException e) {
            if (shouldReconnect(e)) {
                logger.warn("MCP connection lost for {}, reconnecting...", name);
                reconnect();
                return action.get();
            }
            throw e;
        }
    }

    protected boolean shouldReconnect(Throwable throwable) {
        return throwable instanceof McpTransportException
                || throwable instanceof McpTransportSessionClosedException
                || throwable instanceof McpTransportSessionNotFoundException;
    }

    protected void reconnect() {
        close();
        connect();
    }

    protected McpTransportContext buildTransportContext() {
        Map<String, Object> context = new HashMap<>();
        TextMapSetter<Map<String, Object>> setter = (carrier, key, value) -> carrier.put(key, value);
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .inject(Context.current(), context, setter);
        return McpTransportContext.create(context);
    }

    protected McpSchema.CreateMessageResult handleSamplingRequest(McpSchema.CreateMessageRequest request) {
        if (chatClient == null) {
            throw new McpException("ChatClient is not configured for MCP sampling");
        }
        List<Message> messages = new ArrayList<>();
        if (request.messages() != null) {
            for (McpSchema.SamplingMessage message : request.messages()) {
                Message converted = toAgentMessage(message);
                if (converted != null) {
                    messages.add(converted);
                }
            }
        }
        ChatCompleteParams.Builder paramsBuilder = ChatCompleteParams.builder()
                .messages(messages);
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            paramsBuilder.system(request.systemPrompt());
        }
        if (request.temperature() != null) {
            paramsBuilder.temperature(request.temperature());
        }
        if (request.maxTokens() != null) {
            paramsBuilder.maxTokens(request.maxTokens());
        }
        if (request.stopSequences() != null && !request.stopSequences().isEmpty()) {
            paramsBuilder.stop(request.stopSequences().get(0));
        }
        String model = chatClient.getModel();
        if (model != null) {
            paramsBuilder.model(model);
        }

        ChatResponse response = chatClient.chat(paramsBuilder.build());
        String responseText = response != null && response.getMessage() != null
                ? getMessageText(response.getMessage())
                : "";
        McpSchema.Content content = new McpSchema.TextContent(responseText);
        String responseModel = response != null ? response.getModel() : model;
        return new McpSchema.CreateMessageResult(
                McpSchema.Role.ASSISTANT,
                content,
                responseModel,
                mapStopReason(response != null ? response.getFinishReason() : null)
        );
    }

    protected Message toAgentMessage(McpSchema.SamplingMessage message) {
        if (message == null || message.content() == null) {
            return null;
        }
        String text = getTextContent(message.content());
        if ("user".equalsIgnoreCase(String.valueOf(message.role()))) {
            return UserMessage.create(text);
        } else {
            return AssistantMessage.create(text);
        }
    }

    private String getTextContent(Object content) {
        if (content instanceof McpSchema.TextContent) {
            return ((McpSchema.TextContent) content).text();
        }
        if (content instanceof McpSchema.ImageContent) {
            return "[Image content]";
        }
        return String.valueOf(content);
    }

    private String getMessageText(Message message) {
        if (message.getBlocks() == null) return null;
        StringBuilder sb = new StringBuilder();
        for (github.ponyhuang.agentframework.types.block.Block block : message.getBlocks()) {
            if (block instanceof TextBlock) {
                sb.append(((TextBlock) block).getText());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    protected McpSchema.CreateMessageResult.StopReason mapStopReason(String finishReason) {
        if (finishReason == null) {
            return McpSchema.CreateMessageResult.StopReason.UNKNOWN;
        }
        String normalized = finishReason.toLowerCase(Locale.ROOT);
        if ("stop".equals(normalized)) {
            return McpSchema.CreateMessageResult.StopReason.END_TURN;
        }
        if ("length".equals(normalized) || "max_tokens".equals(normalized)) {
            return McpSchema.CreateMessageResult.StopReason.MAX_TOKENS;
        }
        return McpSchema.CreateMessageResult.StopReason.UNKNOWN;
    }

    protected void handleLoggingNotification(McpSchema.LoggingMessageNotification notification) {
        if (notification == null) {
            return;
        }
        String message = notification.data();
        if (message == null) {
            message = "";
        }
        String loggerName = notification.logger() != null ? notification.logger() : name;
        switch (notification.level()) {
            case DEBUG -> logger.debug("[{}] {}", loggerName, message);
            case INFO, NOTICE -> logger.info("[{}] {}", loggerName, message);
            case WARNING -> logger.warn("[{}] {}", loggerName, message);
            case ERROR, CRITICAL, ALERT, EMERGENCY -> logger.error("[{}] {}", loggerName, message);
            default -> logger.info("[{}] {}", loggerName, message);
        }
    }

    protected void handleProgressNotification(McpSchema.ProgressNotification notification) {
        if (notification == null) {
            return;
        }
        String message = notification.message();
        if (message == null || message.isBlank()) {
            message = "progress update";
        }
        Double progress = notification.progress();
        Double total = notification.total();
        if (progress != null && total != null) {
            logger.info("[{}] {} ({}/{})", name, message, progress, total);
        } else if (progress != null) {
            logger.info("[{}] {} ({})", name, message, progress);
        } else {
            logger.info("[{}] {}", name, message);
        }
    }

    protected Set<String> normalizeToolSet(Collection<String> values) {
        if (values == null) {
            return null;
        }
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            normalized.add(normalizeName(value));
        }
        return normalized;
    }

    /**
     * Builder for MCPTool.
     */
    public static abstract class Builder<T extends Builder<T>> {
        protected String name;
        protected String description;
        protected ApprovalMode approvalMode = ApprovalMode.NEVER_REQUIRE;
        protected Collection<String> allowedTools;
        protected Collection<String> approvalTools;
        protected boolean loadTools = true;
        protected boolean loadPrompts = true;
        protected ChatClient chatClient;
        protected Duration requestTimeout;
        protected McpSamplingHandler samplingHandler;
        protected McpLoggingHandler loggingHandler;

        public T name(String name) {
            this.name = name;
            return (T) this;
        }

        public T description(String description) {
            this.description = description;
            return (T) this;
        }

        public T approvalMode(ApprovalMode approvalMode) {
            this.approvalMode = approvalMode;
            return (T) this;
        }

        public T allowedTools(Collection<String> allowedTools) {
            this.allowedTools = allowedTools;
            return (T) this;
        }

        public T approvalTools(Collection<String> approvalTools) {
            this.approvalTools = approvalTools;
            return (T) this;
        }

        public T loadTools(boolean loadTools) {
            this.loadTools = loadTools;
            return (T) this;
        }

        public T loadPrompts(boolean loadPrompts) {
            this.loadPrompts = loadPrompts;
            return (T) this;
        }

        public T chatClient(ChatClient chatClient) {
            this.chatClient = chatClient;
            return (T) this;
        }

        public T requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return (T) this;
        }

        public T samplingHandler(McpSamplingHandler samplingHandler) {
            this.samplingHandler = samplingHandler;
            return (T) this;
        }

        public T loggingHandler(McpLoggingHandler loggingHandler) {
            this.loggingHandler = loggingHandler;
            return (T) this;
        }
    }

    /**
     * Exception for MCP errors.
     */
    public static class McpException extends RuntimeException {
        public McpException(String message) {
            super(message);
        }

        public McpException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

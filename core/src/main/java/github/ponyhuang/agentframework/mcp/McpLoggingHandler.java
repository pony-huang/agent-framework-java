package github.ponyhuang.agentframework.mcp;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Callback for handling MCP logging notifications.
 */
@FunctionalInterface
public interface McpLoggingHandler {
    void onLog(McpSchema.LoggingMessageNotification notification);
}

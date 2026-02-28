package github.ponyhuang.agentframework.mcp;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * Callback for handling MCP sampling requests.
 */
@FunctionalInterface
public interface McpSamplingHandler {
    McpSchema.CreateMessageResult onSample(McpSchema.CreateMessageRequest request);
}

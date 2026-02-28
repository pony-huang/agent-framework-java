package github.ponyhuang.agentframework.middleware;

import github.ponyhuang.agentframework.tools.Tool;

import java.util.Map;
import java.util.function.Function;

/**
 * Middleware for intercepting tool execution.
 * Can modify the arguments/result or terminate early.
 */
public interface FunctionMiddleware {

    /**
     * Processes the tool execution request.
     *
     * @param context the function context
     * @param next   the next handler in the chain
     * @return the tool result
     */
    Object process(FunctionMiddlewareContext context, Function<FunctionMiddlewareContext, Object> next);

    /**
     * Context for function middleware.
     */
    class FunctionMiddlewareContext {
        private final Tool tool;
        private final String toolName;
        private final Map<String, Object> arguments;
        private Object result;
        private java.util.Map<String, Object> metadata;

        public FunctionMiddlewareContext(Tool tool, String toolName, Map<String, Object> arguments) {
            this.tool = tool;
            this.toolName = toolName;
            this.arguments = arguments;
            this.metadata = new java.util.HashMap<>();
        }

        public Tool getTool() {
            return tool;
        }

        public String getToolName() {
            return toolName;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }

        public void setArguments(Map<String, Object> arguments) {
            // Allow modification
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public java.util.Map<String, Object> getMetadata() {
            return metadata;
        }

        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }
    }
}

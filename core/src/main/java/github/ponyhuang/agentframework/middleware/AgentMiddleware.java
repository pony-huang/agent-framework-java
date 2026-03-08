package github.ponyhuang.agentframework.middleware;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Middleware for intercepting agent execution.
 * Can modify the request/response or terminate early.
 */
public interface AgentMiddleware {

    /**
     * Processes the agent execution request.
     *
     * @param context   the agent context
     * @param next     the next handler in the chain
     * @return the agent response
     */
    ChatResponse process(AgentMiddlewareContext context, Function<AgentMiddlewareContext, ChatResponse> next);

    /**
     * Context for agent middleware.
     */
    class AgentMiddlewareContext {
        private final Agent agent;
        private final List<Message> messages;
        private final Map<String, Object> options;
        private ChatResponse response;
        private Map<String, Object> metadata;

        public AgentMiddlewareContext(Agent agent, List<Message> messages, Map<String, Object> options) {
            this.agent = agent;
            this.messages = messages;
            this.options = options;
            this.metadata = new java.util.HashMap<>();
        }

        public Agent getAgent() {
            return agent;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public void setMessages(List<Message> messages) {
            // Allow modification
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public ChatResponse getResponse() {
            return response;
        }

        public void setResponse(ChatResponse response) {
            this.response = response;
        }

        public Map<String, Object> getMetadata() {
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

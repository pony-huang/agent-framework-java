package github.ponyhuang.agentframework.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.Content;
import github.ponyhuang.agentframework.types.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Hook handler that uses LLM for evaluation.
 */
public class PromptHookHandler implements HookHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PromptHookHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String prompt;
    private final ChatClient chatClient;
    private final String model;
    private final Duration timeout;
    private final boolean once;

    public PromptHookHandler(String prompt, ChatClient chatClient) {
        this(prompt, chatClient, null, Duration.ofSeconds(30), false);
    }

    public PromptHookHandler(String prompt, ChatClient chatClient, String model,
                              Duration timeout, boolean once) {
        this.prompt = prompt;
        this.chatClient = chatClient;
        this.model = model;
        this.timeout = timeout;
        this.once = once;
    }

    @Override
    public HookResult execute(HookContext context) {
        if (chatClient == null) {
            LOG.warn("No ChatClient configured for prompt hook");
            return HookResult.allow();
        }

        try {
            // Build the prompt with $ARGUMENTS replaced by context JSON
            String contextJson = MAPPER.writeValueAsString(context.toMap());
            String fullPrompt = prompt.replace("$ARGUMENTS", contextJson);

            // Build messages
            Message userMessage = Message.user(fullPrompt);
            List<Message> messages = List.of(userMessage);

            // Execute chat request
            String responseContent = executeChat(messages);

            // Parse response
            return parseResponse(responseContent);
        } catch (Exception e) {
            LOG.error("Failed to execute prompt hook: {}", e.getMessage());
            return HookResult.allow(); // Non-blocking error
        }
    }

    private String executeChat(List<Message> messages) throws Exception {
        ChatCompleteParams params = ChatCompleteParams.builder()
                .messages(messages)
                .build();

        github.ponyhuang.agentframework.types.ChatResponse response = chatClient.chat(params);

        Message msg = response.getMessage();
        if (msg != null) {
            List<Content> contents = msg.getContents();
            if (contents != null && !contents.isEmpty()) {
                // Return the text content
                for (Content content : contents) {
                    if (content.getType() == Content.ContentType.TEXT) {
                        return content.getText();
                    }
                }
            }
        }

        return "";
    }

    private HookResult parseResponse(String response) {
        if (response == null || response.isEmpty()) {
            return HookResult.allow();
        }

        response = response.trim();

        // Try to parse as JSON
        if (response.startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = MAPPER.readValue(response, Map.class);

                Boolean ok = (Boolean) json.get("ok");
                String reason = (String) json.get("reason");

                if (ok == null) {
                    return HookResult.allow();
                }

                if (ok) {
                    return HookResult.allow();
                } else {
                    return HookResult.block(reason != null ? reason : "Blocked by prompt hook");
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse prompt hook JSON response: {}", e.getMessage());
            }
        }

        // If not JSON, treat as additional context
        HookResult result = HookResult.allow();
        result.setSystemMessage(response);
        return result;
    }

    @Override
    public HookHandlerType getType() {
        return HookHandlerType.PROMPT;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public String getPrompt() {
        return prompt;
    }

    @Override
    public boolean isOnce() {
        return once;
    }

    /**
     * Builder for PromptHookHandler.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String prompt;
        private ChatClient chatClient;
        private String model;
        private Duration timeout = Duration.ofSeconds(30);
        private boolean once = false;

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder chatClient(ChatClient chatClient) {
            this.chatClient = chatClient;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder once(boolean once) {
            this.once = once;
            return this;
        }

        public PromptHookHandler build() {
            return new PromptHookHandler(prompt, chatClient, model, timeout, once);
        }
    }
}

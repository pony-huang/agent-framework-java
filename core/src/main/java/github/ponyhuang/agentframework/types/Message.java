package github.ponyhuang.agentframework.types;

import java.util.List;

/**
 * Represents a message in a conversation.
 */
public class Message {

    private final Role role;
    private final List<Content> contents;
    private final String name;
    private final String toolCallId;

    private Message(Builder builder) {
        this.role = builder.role;
        this.contents = builder.contents;
        this.name = builder.name;
        this.toolCallId = builder.toolCallId;
    }

    /**
     * Creates a user message.
     *
     * @param content the message content
     * @return a new Message instance
     */
    public static Message user(String content) {
        return builder().role(Role.USER).addContent(Content.text(content)).build();
    }

    /**
     * Creates a user message with multiple content parts.
     *
     * @param contents the message contents
     * @return a new Message instance
     */
    public static Message user(List<Content> contents) {
        return builder().role(Role.USER).contents(contents).build();
    }

    /**
     * Creates an assistant message.
     *
     * @param content the message content
     * @return a new Message instance
     */
    public static Message assistant(String content) {
        return builder().role(Role.ASSISTANT).addContent(Content.text(content)).build();
    }

    /**
     * Creates an assistant message with function call.
     *
     * @param functionCall the function call
     * @return a new Message instance
     */
    public static Message assistantFunctionCall(java.util.Map<String, Object> functionCall) {
        return builder().role(Role.ASSISTANT)
                .addContent(Content.fromFunctionCall(functionCall))
                .build();
    }

    /**
     * Creates a system message.
     *
     * @param content the message content
     * @return a new Message instance
     */
    public static Message system(String content) {
        return builder().role(Role.SYSTEM).addContent(Content.text(content)).build();
    }

    /**
     * Creates a tool result message.
     *
     * @param toolCallId the tool call ID
     * @param toolName   the tool name
     * @param result     the result
     * @return a new Message instance
     */
    public static Message tool(String toolCallId, String toolName, Object result) {
        return builder()
                .role(Role.TOOL)
                .toolCallId(toolCallId)
                .addContent(Content.fromFunctionResult(toolCallId, toolName, result))
                .build();
    }

    public Role getRole() {
        return role;
    }

    public List<Content> getContents() {
        return contents;
    }

    public String getName() {
        return name;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    /**
     * Gets the text content of the message.
     *
     * @return the text content, or empty string if no text content
     */
    public String getText() {
        if (contents == null || contents.isEmpty()) {
            return "";
        }
        return contents.stream()
                .filter(c -> c.getType() == Content.ContentType.TEXT)
                .map(Content::getText)
                .findFirst()
                .orElse("");
    }

    /**
     * Gets the first function call in the message, if any.
     *
     * @return the function call, or null if none
     */
    public java.util.Map<String, Object> getFunctionCall() {
        if (contents == null) {
            return null;
        }
        return contents.stream()
                .filter(c -> c.getType() == Content.ContentType.FUNCTION_CALL)
                .map(Content::getFunctionCall)
                .findFirst()
                .orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for Message.
     */
    public static class Builder {
        private Role role;
        private List<Content> contents;
        private String name;
        private String toolCallId;

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder contents(List<Content> contents) {
            this.contents = contents;
            return this;
        }

        public Builder addContent(Content content) {
            if (this.contents == null) {
                this.contents = new java.util.ArrayList<>();
            }
            this.contents.add(content);
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}

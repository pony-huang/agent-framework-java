package github.ponyhuang.agentframework.types;

import java.util.List;
import java.util.Map;

/**
 * Represents the content of a message, which can be text or function call.
 */
public class Content {

    /**
     * The type of content.
     */
    public enum ContentType {
        TEXT("text"),
        IMAGE("image"),
        AUDIO("audio"),
        VIDEO("video"),
        FUNCTION_CALL("function_call"),
        FUNCTION_RESULT("function_result");

        private final String value;

        ContentType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private final ContentType type;
    private final String text;
    private final Map<String, Object> functionCall;
    private final String toolCallId;
    private final String toolName;
    private final Map<String, Object> functionResult;
    private final List<ImageContent> images;

    private Content(Builder builder) {
        this.type = builder.type;
        this.text = builder.text;
        this.functionCall = builder.functionCall;
        this.toolCallId = builder.toolCallId;
        this.toolName = builder.toolName;
        this.functionResult = builder.functionResult;
        this.images = builder.images;
    }

    /**
     * Creates a text content.
     *
     * @param text the text content
     * @return a new Content instance
     */
    public static Content text(String text) {
        return builder().type(ContentType.TEXT).text(text).build();
    }

    /**
     * Creates a function call content.
     *
     * @param functionCall the function call details
     * @return a new Content instance
     */
    public static Content fromFunctionCall(Map<String, Object> functionCall) {
        return builder().type(ContentType.FUNCTION_CALL).functionCall(functionCall).build();
    }

    /**
     * Creates a function result content.
     *
     * @param toolCallId the tool call ID
     * @param toolName   the tool name
     * @param result     the function result
     * @return a new Content instance
     */
    public static Content fromFunctionResult(String toolCallId, String toolName, Object result) {
        return builder()
                .type(ContentType.FUNCTION_RESULT)
                .toolCallId(toolCallId)
                .toolName(toolName)
                .functionResult(result != null ? Map.of("result", result) : null)
                .build();
    }

    public ContentType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getFunctionCall() {
        return functionCall;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public String getToolName() {
        return toolName;
    }

    public Map<String, Object> getFunctionResult() {
        return functionResult;
    }

    public List<ImageContent> getImages() {
        return images;
    }

    /**
     * Gets the function name from a function call.
     *
     * @return the function name, or null if not a function call
     */
    public String getFunctionName() {
        if (functionCall != null && functionCall.containsKey("name")) {
            return (String) functionCall.get("name");
        }
        return null;
    }

    /**
     * Gets the function arguments from a function call.
     *
     * @return the function arguments as a map, or null if not a function call
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFunctionArguments() {
        if (functionCall != null && functionCall.containsKey("arguments")) {
            Object args = functionCall.get("arguments");
            if (args instanceof Map) {
                return (Map<String, Object>) args;
            }
        }
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for Content.
     */
    public static class Builder {
        private ContentType type = ContentType.TEXT;
        private String text;
        private Map<String, Object> functionCall;
        private String toolCallId;
        private String toolName;
        private Map<String, Object> functionResult;
        private List<ImageContent> images;

        public Builder type(ContentType type) {
            this.type = type;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder functionCall(Map<String, Object> functionCall) {
            this.functionCall = functionCall;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder functionResult(Map<String, Object> functionResult) {
            this.functionResult = functionResult;
            return this;
        }

        public Builder images(List<ImageContent> images) {
            this.images = images;
            return this;
        }

        public Content build() {
            return new Content(this);
        }
    }

    /**
     * Represents image content.
     */
    public static class ImageContent {
        private final String url;
        private final String detail;

        public ImageContent(String url) {
            this(url, "auto");
        }

        public ImageContent(String url, String detail) {
            this.url = url;
            this.detail = detail;
        }

        public String getUrl() {
            return url;
        }

        public String getDetail() {
            return detail;
        }
    }
}

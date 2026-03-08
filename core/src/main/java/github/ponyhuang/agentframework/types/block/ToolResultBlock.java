package github.ponyhuang.agentframework.types.block;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ToolResultBlock implements Block {
    private final String type = "tool_result";
    @JsonProperty("tool_use_id")
    private final String toolUseId;
    private final String content;
    private final Boolean isError;

    public ToolResultBlock() {
        this.toolUseId = "";
        this.content = "";
        this.isError = false;
    }

    public ToolResultBlock(String toolUseId, String content) {
        this.toolUseId = toolUseId;
        this.content = content;
        this.isError = false;
    }

    public ToolResultBlock(String toolUseId, String content, Boolean isError) {
        this.toolUseId = toolUseId;
        this.content = content;
        this.isError = isError;
    }

    @Override
    public String getType() {
        return type;
    }

    public String getToolUseId() {
        return toolUseId;
    }

    public String getContent() {
        return content;
    }

    public Boolean getIsError() {
        return isError;
    }

    public static ToolResultBlock of(String toolUseId, String content) {
        return new ToolResultBlock(toolUseId, content);
    }

    public static ToolResultBlock of(String toolUseId, String content, boolean isError) {
        return new ToolResultBlock(toolUseId, content, isError);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToolResultBlock that = (ToolResultBlock) o;
        return type.equals(that.type) &&
                (toolUseId != null ? toolUseId.equals(that.toolUseId) : that.toolUseId == null) &&
                (content != null ? content.equals(that.content) : that.content == null) &&
                (isError != null ? isError.equals(that.isError) : that.isError == null);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + (toolUseId != null ? toolUseId.hashCode() : 0);
        result = 31 * result + (content != null ? content.hashCode() : 0);
        result = 31 * result + (isError != null ? isError.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ToolResultBlock{type='" + type + "', toolUseId='" + toolUseId + "', content='" + content + "', isError=" + isError + "}";
    }
}

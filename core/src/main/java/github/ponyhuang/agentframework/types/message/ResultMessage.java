package github.ponyhuang.agentframework.types.message;

import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.ToolResultBlock;

import java.util.List;
import java.util.Map;

public class ResultMessage extends AbstractMessage {
    private final Map<String, Object> functionResult;

    private ResultMessage(String role, List<Block> blocks, String name, String toolCallId,
                          Map<String, Object> functionResult) {
        super(role, blocks, name, toolCallId);
        this.functionResult = functionResult;
    }

    public static ResultMessage create() {
        return new ResultMessage("tool", List.of(), null, null, null);
    }

    public static ResultMessage create(String toolCallId, String content) {
        ToolResultBlock toolResultBlock = ToolResultBlock.of(toolCallId, content);
        return new ResultMessage("tool", List.of(toolResultBlock), null, toolCallId,
                Map.of("result", content));
    }

    public static ResultMessage create(String toolCallId, String content, boolean isError) {
        ToolResultBlock toolResultBlock = ToolResultBlock.of(toolCallId, content, isError);
        return new ResultMessage("tool", List.of(toolResultBlock), null, toolCallId,
                Map.of("result", content, "is_error", isError));
    }

    public static ResultMessage create(String toolCallId, String toolName, Object result) {
        ToolResultBlock toolResultBlock = ToolResultBlock.of(toolCallId, result != null ? result.toString() : "");
        return new ResultMessage("tool", List.of(toolResultBlock), toolName, toolCallId,
                result != null ? Map.of("result", result) : null);
    }

    public static ResultMessage fromBlocks(List<Block> blocks, String toolCallId) {
        return new ResultMessage("tool", blocks, null, toolCallId, null);
    }

    public static ResultMessage fromFunctionResult(String toolCallId, String toolName, Object result) {
        return create(toolCallId, toolName, result);
    }

    public Map<String, Object> getFunctionResult() {
        return functionResult;
    }

    public String getResultContent() {
        if (functionResult != null && functionResult.containsKey("result")) {
            Object result = functionResult.get("result");
            return result != null ? result.toString() : "";
        }
        if (hasToolResult()) {
            return getBlocks().stream()
                    .filter(b -> b instanceof ToolResultBlock)
                    .map(b -> ((ToolResultBlock) b).getContent())
                    .findFirst()
                    .orElse("");
        }
        return "";
    }

    public boolean isError() {
        if (functionResult != null && functionResult.containsKey("is_error")) {
            return (Boolean) functionResult.get("is_error");
        }
        if (hasToolResult()) {
            return getBlocks().stream()
                    .filter(b -> b instanceof ToolResultBlock)
                    .map(b -> ((ToolResultBlock) b).getIsError())
                    .filter(b -> b != null && b)
                    .findFirst()
                    .orElse(false);
        }
        return false;
    }

    public ResultMessage withToolCallId(String toolCallId) {
        return new ResultMessage(this.role, this.blocks, this.name, toolCallId, this.functionResult);
    }

    public ResultMessage withContent(String content) {
        ToolResultBlock toolResultBlock = ToolResultBlock.of(this.toolCallId, content);
        return new ResultMessage(this.role, List.of(toolResultBlock), this.name, this.toolCallId,
                Map.of("result", content));
    }

    public ResultMessage withBlocks(List<Block> blocks) {
        return new ResultMessage(this.role, blocks, this.name, this.toolCallId, this.functionResult);
    }
}

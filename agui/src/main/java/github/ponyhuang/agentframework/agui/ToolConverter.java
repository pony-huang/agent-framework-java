//package github.ponyhuang.agentframework.agui;
//
//import com.agui.core.tool.Tool;
//import com.agui.core.tool.ToolCall;
//import github.ponyhuang.agentframework.tools.FunctionTool;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//public class ToolConverter {
//
//    public static List<Tool> toAguiTools(List<FunctionTool> functionTools) {
//        if (functionTools == null) {
//            return List.of();
//        }
//        return functionTools.stream()
//                .map(ToolConverter::toAguiTool)
//                .collect(Collectors.toList());
//    }
//
//    public static Tool toAguiTool(FunctionTool functionTool) {
//        if (functionTool == null) {
//            return null;
//        }
//
//        Tool.ToolParameters params = new Tool.ToolParameters(
//                "object",
//                null,
//                null
//        );
//
//        return new Tool(
//                functionTool.getName(),
//                functionTool.getDescription() != null ? functionTool.getDescription() : "",
//                params
//        );
//    }
//
//    public static Map<String, Object> toFrameworkFunctionCall(ToolCall toolCall) {
//        if (toolCall == null) {
//            return Map.of();
//        }
//
//        Map<String, Object> functionCall = new java.util.HashMap<>();
//        functionCall.put("id", toolCall.id());
//        functionCall.put("name", toolCall.function() != null ? toolCall.function().name() : null);
//        functionCall.put("arguments", toolCall.function() != null ? toolCall.function().arguments() : null);
//        return functionCall;
//    }
//
//    public static List<Map<String, Object>> toFrameworkFunctionCalls(List<ToolCall> toolCalls) {
//        if (toolCalls == null) {
//            return List.of();
//        }
//        return toolCalls.stream()
//                .map(ToolConverter::toFrameworkFunctionCall)
//                .collect(Collectors.toList());
//    }
//}

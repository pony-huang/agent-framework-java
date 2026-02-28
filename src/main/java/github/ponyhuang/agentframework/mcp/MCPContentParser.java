package github.ponyhuang.agentframework.mcp;

import github.ponyhuang.agentframework.types.Content;
import io.modelcontextprotocol.spec.McpSchema;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing MCP content into Agent Framework Content objects.
 */
public final class MCPContentParser {

    private MCPContentParser() {
    }

    public static List<Content> parseAll(List<McpSchema.Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return List.of();
        }
        List<Content> parsed = new ArrayList<>();
        for (McpSchema.Content content : contents) {
            Content converted = parse(content);
            if (converted != null) {
                parsed.add(converted);
            }
        }
        return parsed;
    }

    public static Content parse(McpSchema.Content content) {
        if (content == null) {
            return null;
        }
        if (content instanceof McpSchema.TextContent textContent) {
            return Content.text(textContent.text());
        }
        if (content instanceof McpSchema.ImageContent imageContent) {
            String dataUrl = toDataUrl(imageContent.mimeType(), imageContent.data());
            return Content.builder()
                    .type(Content.ContentType.IMAGE)
                    .images(List.of(new Content.ImageContent(dataUrl)))
                    .build();
        }
        if (content instanceof McpSchema.AudioContent audioContent) {
            String dataUrl = toDataUrl(audioContent.mimeType(), audioContent.data());
            return Content.builder()
                    .type(Content.ContentType.AUDIO)
                    .text(dataUrl)
                    .build();
        }
        if (content instanceof McpSchema.EmbeddedResource embeddedResource) {
            return parseResource(embeddedResource.resource());
        }
        Content toolContent = parseToolContent(content);
        if (toolContent != null) {
            return toolContent;
        }
        return Content.text(content.toString());
    }

    public static String toText(List<McpSchema.Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (McpSchema.Content content : contents) {
            String text = toText(content);
            if (text == null || text.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(text);
        }
        return builder.toString();
    }

    public static String toText(McpSchema.Content content) {
        Content parsed = parse(content);
        if (parsed == null) {
            return null;
        }
        switch (parsed.getType()) {
            case TEXT:
            case AUDIO:
            case VIDEO:
                return parsed.getText();
            case IMAGE:
                if (parsed.getImages() == null || parsed.getImages().isEmpty()) {
                    return "";
                }
                return parsed.getImages().get(0).getUrl();
            case FUNCTION_CALL:
                return String.valueOf(parsed.getFunctionCall());
            case FUNCTION_RESULT:
                return String.valueOf(parsed.getFunctionResult());
            default:
                return parsed.getText();
        }
    }

    private static Content parseResource(McpSchema.ResourceContents resource) {
        if (resource == null) {
            return null;
        }
        if (resource instanceof McpSchema.TextResourceContents textResource) {
            return Content.text(textResource.text());
        }
        if (resource instanceof McpSchema.BlobResourceContents blobResource) {
            String dataUrl = toDataUrl(blobResource.mimeType(), blobResource.blob());
            return Content.builder()
                    .type(Content.ContentType.TEXT)
                    .text(dataUrl)
                    .build();
        }
        return Content.text(resource.toString());
    }

    private static Content parseToolContent(McpSchema.Content content) {
        String type = content.type();
        if (type == null) {
            return null;
        }
        if ("tool_use".equalsIgnoreCase(type) || "tooluse".equalsIgnoreCase(type)) {
            Map<String, Object> call = new HashMap<>();
            Object name = readProperty(content, "name", "toolName");
            Object args = readProperty(content, "arguments", "args", "input");
            if (name != null) {
                call.put("name", name.toString());
            }
            if (args instanceof Map) {
                call.put("arguments", args);
            }
            return Content.fromFunctionCall(call);
        }
        if ("tool_result".equalsIgnoreCase(type) || "toolresult".equalsIgnoreCase(type)) {
            Object name = readProperty(content, "name", "toolName");
            Object result = readProperty(content, "result", "content", "output");
            return Content.fromFunctionResult(null, name != null ? name.toString() : null, result);
        }
        return null;
    }

    private static Object readProperty(Object target, String... candidates) {
        for (String candidate : candidates) {
            try {
                Method method = target.getClass().getMethod(candidate);
                return method.invoke(target);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String toDataUrl(String mimeType, String base64) {
        String safeMime = mimeType != null ? mimeType : "application/octet-stream";
        String safeData = base64 != null ? base64 : "";
        return "data:" + safeMime + ";base64," + safeData;
    }
}

package example.agentframework.codeagent.tools;

import github.ponyhuang.agentframework.tools.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tool for editing JSON files with structured operations.
 */
public class JSONEditTool {

    private static final Logger LOG = LoggerFactory.getLogger(JSONEditTool.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String workingDirectory;

    public JSONEditTool(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Get a value from a JSON file by JSONPath-like path.
     */
    @Tool(description = "Get a value from a JSON file using a path. " +
            "Path uses dot notation (e.g., 'data.settings.theme').")
    public String jsonGet(
            @ToolParam(description = "Path to the JSON file") String path,
            @ToolParam(description = "JSONPath-like path to the value (e.g., 'data.settings.theme')") String jsonPath) {
        try {
            Path filePath = resolvePath(path);
            JsonNode root = objectMapper.readTree(Files.readString(filePath));

            JsonNode result = navigateToPath(root, jsonPath);

            if (result == null) {
                return "Error: Path not found: " + jsonPath;
            }

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        } catch (IOException e) {
            LOG.error("Failed to get JSON path {}: {}", jsonPath, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Set a value in a JSON file.
     */
    @Tool(description = "Set a value in a JSON file. Creates nested objects if they don't exist.")
    public String jsonSet(
            @ToolParam(description = "Path to the JSON file") String path,
            @ToolParam(description = "JSONPath-like path to set (e.g., 'data.settings.theme')") String jsonPath,
            @ToolParam(description = "Value to set (JSON string)") String value) {
        try {
            Path filePath = resolvePath(path);

            JsonNode root;
            if (Files.exists(filePath)) {
                root = objectMapper.readTree(Files.readString(filePath));
            } else {
                root = objectMapper.createObjectNode();
            }

            // Navigate to parent and set value
            String[] parts = jsonPath.split("\\.");
            JsonNode current = root;

            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                if (current.has(part)) {
                    current = current.get(part);
                } else {
                    // Create new object node
                    ObjectNode newNode = objectMapper.createObjectNode();
                    ((ObjectNode) current).set(part, newNode);
                    current = newNode;
                }
            }

            // Set the final value
            String lastPart = parts[parts.length - 1];
            JsonNode valueNode = objectMapper.readTree(value);
            ((ObjectNode) current).set(lastPart, valueNode);

            // Write back
            String newContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(filePath, newContent);

            return "Updated " + path + " at path: " + jsonPath;

        } catch (IOException e) {
            LOG.error("Failed to set JSON path {}: {}", jsonPath, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Delete a key from a JSON file.
     */
    @Tool(description = "Delete a key from a JSON file.")
    public String jsonDelete(
            @ToolParam(description = "Path to the JSON file") String path,
            @ToolParam(description = "JSONPath-like path to delete") String jsonPath) {
        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }

            JsonNode root = objectMapper.readTree(Files.readString(filePath));

            String[] parts = jsonPath.split("\\.");
            JsonNode current = root;

            for (int i = 0; i < parts.length - 1; i++) {
                if (!current.has(parts[i])) {
                    return "Error: Path not found: " + jsonPath;
                }
                current = current.get(parts[i]);
            }

            String lastPart = parts[parts.length - 1];
            if (current.has(lastPart)) {
                ((ObjectNode) current).remove(lastPart);
                String newContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                Files.writeString(filePath, newContent);
                return "Deleted " + lastPart + " from " + path;
            } else {
                return "Error: Key not found: " + lastPart;
            }

        } catch (IOException e) {
            LOG.error("Failed to delete JSON path {}: {}", jsonPath, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Add an item to an array in a JSON file.
     */
    @Tool(description = "Add an item to an array in a JSON file.")
    public String jsonAppend(
            @ToolParam(description = "Path to the JSON file") String path,
            @ToolParam(description = "JSONPath-like path to the array") String jsonPath,
            @ToolParam(description = "Item to add (JSON string)") String item) {
        try {
            Path filePath = resolvePath(path);

            JsonNode root;
            if (Files.exists(filePath)) {
                root = objectMapper.readTree(Files.readString(filePath));
            } else {
                root = objectMapper.createObjectNode();
            }

            JsonNode target = navigateToPath(root, jsonPath);

            if (target == null || !target.isArray()) {
                return "Error: Path is not an array: " + jsonPath;
            }

            JsonNode itemNode = objectMapper.readTree(item);
            ((ArrayNode) target).add(itemNode);

            String newContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(filePath, newContent);

            return "Added item to array at: " + jsonPath;

        } catch (IOException e) {
            LOG.error("Failed to append to JSON array {}: {}", jsonPath, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * View the entire JSON file.
     */
    @Tool(description = "View a JSON file with pretty formatting.")
    public String jsonView(@ToolParam(description = "Path to the JSON file") String path) {
        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }

            JsonNode root = objectMapper.readTree(Files.readString(filePath));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        } catch (IOException e) {
            LOG.error("Failed to view JSON {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Validate JSON file syntax.
     */
    @Tool(description = "Validate JSON file syntax.")
    public String jsonValidate(@ToolParam(description = "Path to the JSON file") String path) {
        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }

            objectMapper.readTree(Files.readString(filePath));
            return "Valid JSON";

        } catch (JsonProcessingException e) {
            return "Invalid JSON: " + e.getMessage();
        } catch (IOException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    private JsonNode navigateToPath(JsonNode root, String jsonPath) {
        String[] parts = jsonPath.split("\\.");
        JsonNode current = root;

        for (String part : parts) {
            if (current == null || !current.has(part)) {
                return null;
            }
            current = current.get(part);
        }

        return current;
    }

    private Path resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }

        return Paths.get(workingDirectory, path).normalize();
    }
}
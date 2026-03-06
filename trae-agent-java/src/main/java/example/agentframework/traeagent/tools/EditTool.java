package example.agentframework.traeagent.tools;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Tool for viewing, creating, and editing files.
 * Supports view, create, str_replace, and insert operations.
 */
public class EditTool {

    private static final Logger LOG = LoggerFactory.getLogger(EditTool.class);

    private final String workingDirectory;

    public EditTool(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * View a file or directory.
     */
    @Tool(description = "View the contents of a file or directory. " +
            "For files, shows the full content. For directories, lists files. " +
            "Use path relative to working directory.")
    public String view(@ToolParam(description = "Path to file or directory to view") String path) {
        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return "Error: Path does not exist: " + path;
            }

            if (Files.isDirectory(filePath)) {
                // List directory contents
                StringBuilder result = new StringBuilder("Directory: " + path + "\n");
                try (var stream = Files.list(filePath)) {
                    stream.sorted()
                            .forEach(p -> {
                                String type = Files.isDirectory(p) ? "[DIR]" : "[FILE]";
                                result.append(type).append(" ").append(p.getFileName()).append("\n");
                            });
                }
                return result.toString();
            }

            // Read file content
            String content = Files.readString(filePath);
            return "File: " + path + "\n" + content;

        } catch (IOException e) {
            LOG.error("Failed to view {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * View a specific range of lines in a file.
     */
    @Tool(description = "View specific lines from a file. Provide start and end line numbers.")
    public String viewRange(
            @ToolParam(description = "Path to file") String path,
            @ToolParam(description = "Start line number") int startLine,
            @ToolParam(description = "End line number") int endLine) {
        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }

            List<String> lines = Files.readAllLines(filePath);

            if (startLine < 1 || startLine > lines.size()) {
                return "Error: Invalid start line: " + startLine;
            }

            if (endLine < startLine || endLine > lines.size()) {
                return "Error: Invalid end line: " + endLine;
            }

            StringBuilder result = new StringBuilder("File: " + path + " (lines " + startLine + "-" + endLine + ")\n");
            for (int i = startLine - 1; i < endLine; i++) {
                result.append(String.format("%5d: %s\n", i + 1, lines.get(i)));
            }
            return result.toString();

        } catch (IOException e) {
            LOG.error("Failed to view range {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Create a new file with content.
     */
    @Tool(description = "Create a new file with the given content.")
    public String create(
            @ToolParam(description = "Path to the new file to create") String path,
            @ToolParam(description = "Content to write to the file") String content) {
        try {
            Path filePath = resolvePath(path);

            if (Files.exists(filePath)) {
                return "Error: File already exists: " + path;
            }

            // Create parent directories if needed
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(filePath, content);
            LOG.info("Created file: {}", path);
            return "Created file: " + path;

        } catch (IOException e) {
            LOG.error("Failed to create {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Replace text in a file.
     */
    @Tool(description = "Replace a specific string in a file with new content. " +
            "Use OLD_STRING to specify what to replace and NEW_STRING for the replacement.")
    public String strReplace(
            @ToolParam(description = "Path to the file") String path,
            @ToolParam(description = "The exact string to find and replace") String oldString,
            @ToolParam(description = "The new string to replace it with") String newString) {
        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }

            String content = Files.readString(filePath);

            if (!content.contains(oldString)) {
                return "Error: Old string not found in file. Make sure to use the exact text from the file.";
            }

            String newContent = content.replace(oldString, newString);
            Files.writeString(filePath, newContent);

            LOG.info("Edited file: {}", path);
            return "Edited file: " + path;

        } catch (IOException e) {
            LOG.error("Failed to edit {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Insert text at a specific line.
     */
    @Tool(description = "Insert new content at a specific line number in a file.")
    public String insert(
            @ToolParam(description = "Path to the file") String path,
            @ToolParam(description = "Line number to insert after") int afterLine,
            @ToolParam(description = "Content to insert") String content) {
        try {
            Path filePath = resolvePath(path);

            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }

            List<String> lines = Files.readAllLines(filePath);

            if (afterLine < 0 || afterLine > lines.size()) {
                return "Error: Invalid line number: " + afterLine;
            }

            List<String> newLines = new ArrayList<>(lines);

            // Split content into lines and add
            String[] contentLines = content.split("\n");
            for (int i = contentLines.length - 1; i >= 0; i--) {
                newLines.add(afterLine, contentLines[i]);
            }

            Files.write(filePath, newLines);

            LOG.info("Inserted at line {} in file: {}", afterLine, path);
            return "Inserted at line " + afterLine + " in file: " + path;

        } catch (IOException e) {
            LOG.error("Failed to insert in {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Search for text in files.
     */
    @Tool(description = "Search for a string in files within a directory. Returns matching lines with file paths.")
    public String grep(
            @ToolParam(description = "Pattern to search for") String pattern,
            @ToolParam(description = "Directory to search in (default: working directory)", required = false) String dir,
            @ToolParam(description = "File pattern to match (e.g., *.java)", required = false) String filePattern) {
        try {
            Path searchDir = dir != null ? resolvePath(dir) : Paths.get(workingDirectory);

            if (!Files.exists(searchDir) || !Files.isDirectory(searchDir)) {
                return "Error: Directory does not exist: " + searchDir;
            }

            StringBuilder result = new StringBuilder();
            String glob = filePattern != null ? filePattern : "*";

            try (var stream = Files.walk(searchDir)) {
                stream.filter(p -> Files.isRegularFile(p))
                        .filter(p -> p.toString().matches(".*" + glob.replace("*", ".*")))
                        .forEach(p -> {
                            try {
                                List<String> lines = Files.readAllLines(p);
                                for (int i = 0; i < lines.size(); i++) {
                                    if (lines.get(i).contains(pattern)) {
                                        result.append(p).append(":").append(i + 1).append(": ")
                                                .append(lines.get(i)).append("\n");
                                    }
                                }
                            } catch (IOException e) {
                                // Skip files that can't be read
                            }
                        });
            }

            return result.length() > 0 ? result.toString() : "No matches found.";
        } catch (IOException e) {
            LOG.error("Search failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
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
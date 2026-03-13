package github.ponyhuang.agentframework.tools.builtins;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * Built-in tools for file system operations.
 * Provides read, write, edit, and glob capabilities.
 */
public class FileSystemTools {

    private static final Logger LOG = LoggerFactory.getLogger(FileSystemTools.class);

    private final String workingDirectory;

    public FileSystemTools(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Read the contents of a file.
     */
    @Tool(description = "Read the contents of a file and return it as a string.")
    public String read(@ToolParam(description = "Path to the file to read") String path) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }
            if (Files.isDirectory(filePath)) {
                return "Error: Path is a directory, not a file: " + path;
            }
            return Files.readString(filePath);
        } catch (IOException e) {
            LOG.error("Failed to read file {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Read a specific range of lines from a file.
     */
    @Tool(description = "Read a specific range of lines from a file.")
    public String readRange(
            @ToolParam(description = "Path to the file to read") String path,
            @ToolParam(description = "Line number to start from (1-based)") int offset,
            @ToolParam(description = "Number of lines to read") int limit) {
        try {
            Path filePath = resolvePath(path);
            if (!Files.exists(filePath)) {
                return "Error: File does not exist: " + path;
            }

            List<String> allLines = Files.readAllLines(filePath);

            // Convert to 0-based indexing
            int start = Math.max(0, offset - 1);
            int end = Math.min(allLines.size(), start + limit);

            if (start >= allLines.size()) {
                return "Error: Offset exceeds file length";
            }

            StringBuilder result = new StringBuilder();
            for (int i = start; i < end; i++) {
                result.append(i + 1).append(": ").append(allLines.get(i)).append("\n");
            }
            return result.toString();
        } catch (IOException e) {
            LOG.error("Failed to read file {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Write content to a file.
     */
    @Tool(description = "Write content to a file. Creates the file if it doesn't exist, overwrites if it does.")
    public String write(
            @ToolParam(description = "Path to the file to write") String path,
            @ToolParam(description = "Content to write to the file") String content) {
        try {
            Path filePath = resolvePath(path);

            // Create parent directories if needed
            Path parent = filePath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.writeString(filePath, content);
            LOG.info("Wrote to file: {}", path);
            return "Successfully wrote to file: " + path;
        } catch (IOException e) {
            LOG.error("Failed to write file {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Edit a file by replacing old text with new text.
     */
    @Tool(description = "Edit a file by replacing old text with new text. " +
            "Use OLD_STRING to specify what to replace and NEW_STRING for the replacement.")
    public String edit(
            @ToolParam(description = "Path to the file to edit") String path,
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
            return "Successfully edited file: " + path;
        } catch (IOException e) {
            LOG.error("Failed to edit file {}: {}", path, e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Find files matching a glob pattern.
     */
    @Tool(description = "Find files matching a glob pattern. Returns a list of matching file paths.")
    public List<String> glob(
            @ToolParam(description = "Glob pattern to match (e.g., *.java, **/*.txt)") String pattern,
            @ToolParam(description = "Root directory to search in (optional, defaults to working directory)", required = false) String dir) {
        try {
            Path searchDir = dir != null ? resolvePath(dir) : Paths.get(workingDirectory);

            if (!Files.exists(searchDir)) {
                return List.of("Error: Directory does not exist: " + searchDir);
            }

            List<String> matches = new ArrayList<>();
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

            Files.walkFileTree(searchDir, new SimpleFileVisitor<>() {
                @NotNull
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    Path relativePath = searchDir.relativize(file);
                    if (matcher.matches(relativePath) || matcher.matches(file.getFileName())) {
                        matches.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            return matches;
        } catch (IOException e) {
            LOG.error("Failed to glob pattern {}: {}", pattern, e.getMessage());
            return List.of("Error: " + e.getMessage());
        }
    }

    private Path resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return p.normalize();
        }

        return Paths.get(workingDirectory, path).normalize();
    }
}

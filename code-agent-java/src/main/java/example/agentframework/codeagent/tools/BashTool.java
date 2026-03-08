package example.agentframework.codeagent.tools;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Tool for executing shell commands.
 * Uses ProcessBuilder for command execution.
 */
public class BashTool {

    private static final Logger LOG = LoggerFactory.getLogger(BashTool.class);

    private final String workingDirectory;

    public BashTool(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Tool(description = "Execute a shell command. Use this to run programs, git commands, etc. " +
            "Returns the command output or error.")
    public String bash(@ToolParam(description = "The shell command to execute") String command) {
        return executeCommand(command);
    }

    @Tool(description = "Execute a shell command with timeout. Use this for longer running commands.")
    public String bashWithTimeout(
            @ToolParam(description = "The shell command to execute") String command,
            @ToolParam(description = "Timeout in seconds (default: 60)", required = false) Integer timeoutSeconds) {
        int timeout = timeoutSeconds != null ? timeoutSeconds : 60;
        return executeCommand(command, timeout);
    }

    private String executeCommand(String command) {
        return executeCommand(command, 60);
    }

    private String executeCommand(String command, int timeoutSeconds) {
        LOG.info("Executing command: {}", command);

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("bash", "-c", command);

            // Set working directory
            if (workingDirectory != null && !workingDirectory.isEmpty()) {
                Path workDir = Paths.get(workingDirectory);
                if (Files.exists(workDir)) {
                    pb.directory(workDir.toFile());
                }
            }

            pb.redirectErrorStream(true);
            pb.environment().putAll(System.getenv());

            Process process = pb.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Wait for completion with timeout
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return "Error: Command timed out after " + timeoutSeconds + " seconds";
            }

            int exitCode = process.exitValue();
            String result = output.toString();

            if (exitCode != 0) {
                LOG.warn("Command exited with code: {}", exitCode);
                return "Exit code: " + exitCode + "\n" + result;
            }

            return result.isEmpty() ? "Command completed successfully." : result;

        } catch (Exception e) {
            LOG.error("Command execution failed: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Execute a command and return exit code along with output.
     */
    @Tool(description = "Execute a shell command and get detailed result including exit code.")
    public Map<String, Object> bashDetailed(
            @ToolParam(description = "The shell command to execute") String command,
            @ToolParam(description = "Timeout in seconds (default: 60)", required = false) Integer timeoutSeconds) {
        int timeout = timeoutSeconds != null ? timeoutSeconds : 60;

        LOG.info("Executing command with details: {}", command);

        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("bash", "-c", command);

            if (workingDirectory != null && !workingDirectory.isEmpty()) {
                Path workDir = Paths.get(workingDirectory);
                if (Files.exists(workDir)) {
                    pb.directory(workDir.toFile());
                }
            }

            pb.redirectErrorStream(true);
            pb.environment().putAll(System.getenv());

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(timeout, TimeUnit.SECONDS);

            if (!completed) {
                process.destroyForcibly();
                return Map.of(
                        "success", false,
                        "error", "Command timed out after " + timeout + " seconds"
                );
            }

            int exitCode = process.exitValue();
            String result = output.toString();

            return Map.of(
                    "success", exitCode == 0,
                    "exit_code", exitCode,
                    "output", result
            );

        } catch (Exception e) {
            LOG.error("Command execution failed: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}
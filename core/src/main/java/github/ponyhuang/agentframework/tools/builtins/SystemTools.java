package github.ponyhuang.agentframework.tools.builtins;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Built-in tools for system operations.
 * Provides bash execution, user questioning, and plan mode capabilities.
 */
public class SystemTools {

    private static final Logger LOG = LoggerFactory.getLogger(SystemTools.class);

    private final String workingDirectory;

    public SystemTools(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Execute a bash command.
     */
    @Tool(description = "Execute a shell command. Use this to run programs, git commands, etc. " +
            "Returns the command output or error.")
    public String bash(@ToolParam(description = "The shell command to execute") String command) {
        return executeCommand(command, 60);
    }

    /**
     * Execute a bash command with timeout.
     */
    @Tool(description = "Execute a shell command with a timeout. Use this for longer running commands.")
    public String bashWithTimeout(
            @ToolParam(description = "The shell command to execute") String command,
            @ToolParam(description = "Timeout in seconds (default: 60)", required = false) Integer timeoutSeconds) {
        int timeout = timeoutSeconds != null ? timeoutSeconds : 60;
        return executeCommand(command, timeout);
    }

    /**
     * Execute a command and return detailed result including exit code.
     */
    @Tool(description = "Execute a shell command and get detailed result including exit code, success status, and output.")
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

    /**
     * Ask a question to the user.
     */
    @Tool(description = "Ask a question to the user and present options for them to choose from.")
    public String askUser(
            @ToolParam(description = "The question to ask the user") String question,
            @ToolParam(description = "Available options (comma-separated)", required = false) String options,
            @ToolParam(description = "Whether multiple selections are allowed", required = false) Boolean multiSelect) {

        StringBuilder response = new StringBuilder();
        response.append("## User Question\n\n");
        response.append(question).append("\n\n");

        if (options != null && !options.isEmpty()) {
            response.append("**Options:**\n");
            String[] opts = options.split(",");
            for (int i = 0; i < opts.length; i++) {
                response.append(i + 1).append(". ").append(opts[i].trim()).append("\n");
            }
            response.append("\n");
        } else {
            response.append("_Please provide options when using this tool._\n");
        }

        if (multiSelect != null && multiSelect) {
            response.append("_Multiple selections allowed._\n");
        }

        response.append("\n**Note:** This is a placeholder. In a real implementation, " +
                "this would pause and wait for user input.");

        return response.toString();
    }

    /**
     * Enter plan mode.
     */
    @Tool(description = "Enter plan mode to plan implementation steps before executing. Use when the task requires careful planning.")
    public String planMode(
            @ToolParam(description = "Reason for entering plan mode") String reason) {

        return "## Plan Mode Initiated\n\n" +
                "Reason: " + reason + "\n\n" +
                "Plan mode is a UI-level feature. In a real implementation, " +
                "this would transition the interface to a planning state where " +
                "implementation steps can be drafted and reviewed before execution.\n\n" +
                "Use `/opsx:new` or `/opsx:ff` to create a new change for the plan.";
    }

    private String executeCommand(String command, int timeoutSeconds) {
        LOG.info("Executing command: {}", command);

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
}

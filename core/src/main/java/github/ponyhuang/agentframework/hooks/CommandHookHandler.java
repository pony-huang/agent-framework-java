package github.ponyhuang.agentframework.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.ponyhuang.agentframework.hooks.event.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Hook handler that executes shell commands.
 */
public class CommandHookHandler implements HookHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CommandHookHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String command;
    private final Duration timeout;
    private final boolean async;
    private final boolean once;
    private final String statusMessage;
    private final ExecutorService executorService;

    public CommandHookHandler(String command) {
        this(command, Duration.ofSeconds(600), false, false, null, null);
    }

    public CommandHookHandler(String command, Duration timeout, boolean async, boolean once,
                               String statusMessage, ExecutorService executorService) {
        this.command = command;
        this.timeout = timeout;
        this.async = async;
        this.once = once;
        this.statusMessage = statusMessage;
        this.executorService = executorService;
    }

    @Override
    public HookResult execute(BaseEvent event) {
        try {
            String jsonInput = MAPPER.writeValueAsString(event.toMap());

            if (async) {
                return executeAsync(jsonInput);
            } else {
                return executeSync(jsonInput);
            }
        } catch (Exception e) {
            LOG.error("Failed to execute command hook: {}", e.getMessage());
            return HookResult.deny("Hook execution failed: " + e.getMessage());
        }
    }

    private HookResult executeSync(String jsonInput) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.redirectErrorStream(false);

        Process process = pb.start();

        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
            writer.write(jsonInput);
            writer.flush();
        }

        ExecutorService execService = executorService != null ? executorService : Executors.newSingleThreadExecutor();
        Future<String> outputFuture = execService.submit(() -> {
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            return output.toString();
        });

        Future<String> errorFuture = execService.submit(() -> {
            StringBuilder error = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    error.append(line).append("\n");
                }
            }
            return error.toString();
        });

        int exitCode;
        try {
            boolean finished = process.waitFor(timeout.getSeconds(), TimeUnit.SECONDS);
            exitCode = finished ? process.exitValue() : -1;
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return HookResult.deny("Hook execution interrupted");
        }

        String output = "";
        String errorOutput = "";

        try {
            output = outputFuture.get(1, TimeUnit.SECONDS);
            errorOutput = errorFuture.get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Ignore timeout on reading output
        }

        if (executorService == null) {
            execService.shutdown();
        }

        LOG.debug("Command hook exit code: {}, output: {}", exitCode, output);

        // Process exit code
        if (exitCode == 0) {
            return parseOutput(output);
        } else if (exitCode == 2) {
            // Blocking error
            return HookResult.block(errorOutput.isEmpty() ? "Blocked by hook" : errorOutput);
        } else {
            // Non-blocking error
            HookResult result = HookResult.allow();
            result.setSystemMessage(errorOutput.isEmpty() ? "Hook error" : errorOutput);
            return result;
        }
    }

    private HookResult executeAsync(String jsonInput) {
        // Run in background
        ExecutorService execService = executorService != null ? executorService : Executors.newCachedThreadPool();
        execService.submit(() -> {
            try {
                executeSync(jsonInput);
            } catch (Exception e) {
                LOG.error("Async hook execution failed: {}", e.getMessage());
            }
        });

        if (executorService == null) {
            // Don't shutdown for async - let it run in background
        }

        // Async hooks don't block - return allow immediately
        return HookResult.allow();
    }

    private HookResult parseOutput(String output) {
        if (output == null || output.isEmpty()) {
            return HookResult.allow();
        }

        output = output.trim();

        // Try to parse as JSON
        if (output.startsWith("{")) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> json = MAPPER.readValue(output, Map.class);
                return parseJsonResult(json);
            } catch (Exception e) {
                LOG.warn("Failed to parse hook JSON output: {}", e.getMessage());
            }
        }

        // Plain text output - treat as additional context
        HookResult result = HookResult.allow();
        result.setSystemMessage(output);
        return result;
    }

    @SuppressWarnings("unchecked")
    private HookResult parseJsonResult(Map<String, Object> json) {
        HookResult result = new HookResult();

        // Universal fields
        if (json.containsKey("continue")) {
            result.setShouldContinue((Boolean) json.get("continue"));
        }
        if (json.containsKey("stopReason")) {
            result.setStopReason((String) json.get("stopReason"));
        }
        if (json.containsKey("suppressOutput")) {
            result.setSuppressOutput((Boolean) json.get("suppressOutput"));
        }
        if (json.containsKey("systemMessage")) {
            result.setSystemMessage((String) json.get("systemMessage"));
        }

        // Top-level decision (for PostToolUse, Stop, etc.)
        if (json.containsKey("decision")) {
            String decision = (String) json.get("decision");
            result.setAllow(!"block".equals(decision));
            if (json.containsKey("reason")) {
                result.setReason((String) json.get("reason"));
            }
        }

        // hookSpecificOutput for PreToolUse, PermissionRequest
        if (json.containsKey("hookSpecificOutput")) {
            Map<String, Object> hookSpecific = (Map<String, Object>) json.get("hookSpecificOutput");
            result.setHookSpecificOutput(hookSpecific);

            String eventName = (String) hookSpecific.get("hookEventName");
            if ("PreToolUse".equals(eventName)) {
                String permissionDecision = (String) hookSpecific.get("permissionDecision");
                result.setAllow(!"deny".equals(permissionDecision));
                if (hookSpecific.containsKey("permissionDecisionReason")) {
                    result.setReason((String) hookSpecific.get("permissionDecisionReason"));
                }
            } else if ("PermissionRequest".equals(eventName)) {
                Map<String, Object> decision = (Map<String, Object>) hookSpecific.get("decision");
                if (decision != null) {
                    String behavior = (String) decision.get("behavior");
                    result.setAllow(!"deny".equals(behavior));
                }
            }
        }

        // additionalContext for SessionStart, UserPromptSubmit, etc.
        if (json.containsKey("additionalContext")) {
            result.setAdditionalContext((String) json.get("additionalContext"));
        }

        return result;
    }

    @Override
    public HookHandlerType getType() {
        return HookHandlerType.COMMAND;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    @Override
    public boolean isOnce() {
        return once;
    }

    /**
     * Builder for CommandHookHandler.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String command;
        private Duration timeout = Duration.ofSeconds(600);
        private boolean async = false;
        private boolean once = false;
        private String statusMessage;
        private ExecutorService executorService;

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        public Builder once(boolean once) {
            this.once = once;
            return this;
        }

        public Builder statusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;
        }

        public Builder executorService(ExecutorService executorService) {
            this.executorService = executorService;
            return this;
        }

        public CommandHookHandler build() {
            return new CommandHookHandler(command, timeout, async, once, statusMessage, executorService);
        }
    }
}

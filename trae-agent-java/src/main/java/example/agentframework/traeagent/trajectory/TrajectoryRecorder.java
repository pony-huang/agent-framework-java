package example.agentframework.traeagent.trajectory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import github.ponyhuang.agentframework.types.ChatCompleteParams;
import github.ponyhuang.agentframework.types.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Records agent execution trajectory for debugging and analysis.
 */
public class TrajectoryRecorder {

    private static final Logger LOG = LoggerFactory.getLogger(TrajectoryRecorder.class);

    private final ObjectMapper objectMapper;
    private final String trajectoryPath;
    private final boolean enabled;

    private final Queue<TrajectoryEvent> events = new ConcurrentLinkedQueue<>();
    private final String sessionId;
    private final Instant startTime;

    public TrajectoryRecorder(String trajectoryPath, boolean enabled) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.trajectoryPath = trajectoryPath;
        this.enabled = enabled;
        this.sessionId = UUID.randomUUID().toString();
        this.startTime = Instant.now();
    }

    /**
     * Record an LLM call.
     */
    public void recordLLMCall(ChatCompleteParams params) {
        if (!enabled) return;

        Map<String, Object> data = new HashMap<>();
        data.put("model", params.getModel());
        data.put("messages", params.getMessages().size());

        if (params.getTools() != null) {
            data.put("tools_count", params.getTools().size());
        }

        addEvent(TrajectoryEvent.Type.LLM_CALL, data);
    }

    /**
     * Record an LLM response.
     */
    public void recordLLMResponse(ChatResponse response) {
        if (!enabled) return;

        Map<String, Object> data = new HashMap<>();
        data.put("model", response.getModel());
        data.put("finish_reason", response.getFinishReason());

        if (response.getUsage() != null) {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("prompt_tokens", response.getUsage().getPromptTokens());
            usage.put("completion_tokens", response.getUsage().getCompletionTokens());
            usage.put("total_tokens", response.getUsage().getTotalTokens());
            data.put("usage", usage);
        }

        // Check if response has function call
        if (response.getMessage() != null && response.getMessage().getFunctionCall() != null) {
            data.put("has_function_call", true);
            data.put("function_name", response.getMessage().getFunctionCall().get("name"));
        }

        addEvent(TrajectoryEvent.Type.LLM_RESPONSE, data);
    }

    /**
     * Record a tool call.
     */
    public void recordToolCall(String toolName, Map<String, Object> arguments) {
        if (!enabled) return;

        Map<String, Object> data = new HashMap<>();
        data.put("tool_name", toolName);
        data.put("arguments", arguments);

        addEvent(TrajectoryEvent.Type.TOOL_CALL, data);
    }

    /**
     * Record a tool result.
     */
    public void recordToolResult(String toolName, Object result) {
        if (!enabled) return;

        Map<String, Object> data = new HashMap<>();
        data.put("tool_name", toolName);

        // Truncate long results
        String resultStr = result != null ? result.toString() : "null";
        if (resultStr.length() > 1000) {
            data.put("result", resultStr.substring(0, 1000) + "... [truncated]");
        } else {
            data.put("result", resultStr);
        }

        addEvent(TrajectoryEvent.Type.TOOL_RESULT, data);
    }

    /**
     * Add a custom event.
     */
    public void addCustomEvent(String type, Map<String, Object> data) {
        if (!enabled) return;
        addEvent(TrajectoryEvent.Type.CUSTOM, data, type);
    }

    private void addEvent(TrajectoryEvent.Type type, Map<String, Object> data) {
        addEvent(type, data, null);
    }

    private void addEvent(TrajectoryEvent.Type type, Map<String, Object> data, String customType) {
        TrajectoryEvent event = new TrajectoryEvent();
        event.type = type.name();
        if (customType != null) {
            event.type = customType;
        }
        event.timestamp = Instant.now().toString();
        event.data = data;

        events.add(event);

        // Log for debugging
        LOG.debug("Trajectory event: {} - {}", type, data);
    }

    /**
     * Finish recording and save to file.
     */
    public void finish() {
        if (!enabled) return;

        try {
            Trajectory trajectory = new Trajectory();
            trajectory.session_id = sessionId;
            trajectory.start_time = startTime.toString();
            trajectory.end_time = Instant.now().toString();
            trajectory.events = new ArrayList<>(events);

            // Ensure directory exists
            Path path = Paths.get(trajectoryPath);
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            // Write to file
            String json = objectMapper.writeValueAsString(trajectory);
            Files.writeString(path, json);

            LOG.info("Trajectory saved to: {}", trajectoryPath);

        } catch (IOException e) {
            LOG.error("Failed to save trajectory: {}", e.getMessage());
        }
    }

    /**
     * Get the session ID.
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Get event count.
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * Data classes for JSON serialization.
     */
    private static class Trajectory {
        public String session_id;
        public String start_time;
        public String end_time;
        public List<TrajectoryEvent> events;
    }

    private static class TrajectoryEvent {
        enum Type {
            LLM_CALL, LLM_RESPONSE, TOOL_CALL, TOOL_RESULT, CUSTOM
        }

        public String type;
        public String timestamp;
        public Map<String, Object> data;
    }
}
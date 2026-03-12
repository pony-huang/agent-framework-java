package github.ponyhuang.agentframework.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import github.ponyhuang.agentframework.hooks.event.BaseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hook handler that sends HTTP POST requests.
 */
public class HttpHookHandler implements HookHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpHookHandler.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{?(\\w+)\\}?");

    private final String url;
    private final Map<String, String> headers;
    private final Duration timeout;
    private final boolean async;
    private final boolean once;
    private final java.util.Set<String> allowedEnvVars;
    private final ExecutorService executorService;
    private final HttpClient httpClient;

    public HttpHookHandler(String url, Map<String, String> headers, Duration timeout,
                           boolean async, boolean once, java.util.Set<String> allowedEnvVars) {
        this.url = url;
        this.headers = headers;
        this.timeout = timeout;
        this.async = async;
        this.once = once;
        this.allowedEnvVars = allowedEnvVars;
        this.executorService = null;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public HookResult execute(BaseEvent event) {
        try {
            String jsonInput = MAPPER.writeValueAsString(event.toMap());

            // Build request with headers
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput));

            // Add custom headers with env var interpolation
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String value = interpolateEnvVars(entry.getValue());
                    requestBuilder.header(entry.getKey(), value);
                }
            }

            HttpRequest request = requestBuilder.build();

            if (async) {
                executeAsync(request);
                return HookResult.allow();
            } else {
                return executeSync(request);
            }
        } catch (Exception e) {
            LOG.error("Failed to execute HTTP hook: {}", e.getMessage());
            return HookResult.allow(); // Non-blocking error
        }
    }

    private String interpolateEnvVars(String value) {
        if (value == null || allowedEnvVars == null) {
            return value;
        }

        Matcher matcher = ENV_VAR_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            if (allowedEnvVars.contains(varName)) {
                String envValue = System.getenv(varName);
                matcher.appendReplacement(result, envValue != null ? Matcher.quoteReplacement(envValue) : "");
            } else {
                // Replace with empty string if not allowed
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private HookResult executeSync(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int statusCode = response.statusCode();
            String body = response.body();

            LOG.debug("HTTP hook response: status={}", statusCode);

            // 2xx = success
            if (statusCode >= 200 && statusCode < 300) {
                if (body == null || body.isEmpty()) {
                    return HookResult.allow();
                }

                body = body.trim();
                if (body.startsWith("{")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> json = MAPPER.readValue(body, Map.class);
                    return parseJsonResult(json);
                } else {
                    // Plain text response
                    HookResult result = HookResult.allow();
                    result.setSystemMessage(body);
                    return result;
                }
            } else {
                // Non-2xx = non-blocking error
                HookResult result = HookResult.allow();
                result.setSystemMessage("HTTP hook error: " + statusCode + " - " + body);
                return result;
            }
        } catch (Exception e) {
            LOG.error("HTTP hook execution failed: {}", e.getMessage());
            HookResult result = HookResult.allow();
            result.setSystemMessage("HTTP hook error: " + e.getMessage());
            return result;
        }
    }

    private void executeAsync(HttpRequest request) {
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    LOG.debug("Async HTTP hook response: status={}", response.statusCode());
                })
                .exceptionally(e -> {
                    LOG.error("Async HTTP hook failed: {}", e.getMessage());
                    return null;
                });
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
        if (json.containsKey("systemMessage")) {
            result.setSystemMessage((String) json.get("systemMessage"));
        }

        // Top-level decision
        if (json.containsKey("decision")) {
            String decision = (String) json.get("decision");
            result.setAllow(!"block".equals(decision));
            if (json.containsKey("reason")) {
                result.setReason((String) json.get("reason"));
            }
        }

        // hookSpecificOutput
        if (json.containsKey("hookSpecificOutput")) {
            Map<String, Object> hookSpecific = (Map<String, Object>) json.get("hookSpecificOutput");
            result.setHookSpecificOutput(hookSpecific);

            String eventName = (String) hookSpecific.get("hookEventName");
            if ("PreToolUse".equals(eventName)) {
                String permissionDecision = (String) hookSpecific.get("permissionDecision");
                result.setAllow(!"deny".equals(permissionDecision));
            }
        }

        return result;
    }

    @Override
    public HookHandlerType getType() {
        return HookHandlerType.HTTP;
    }

    @Override
    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public String getUrl() {
        return url;
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
     * Builder for HttpHookHandler.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String url;
        private Map<String, String> headers;
        private Duration timeout = Duration.ofSeconds(30);
        private boolean async = false;
        private boolean once = false;
        private java.util.Set<String> allowedEnvVars;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
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

        public Builder allowedEnvVars(java.util.Set<String> allowedEnvVars) {
            this.allowedEnvVars = allowedEnvVars;
            return this;
        }

        public HttpHookHandler build() {
            return new HttpHookHandler(url, headers, timeout, async, once, allowedEnvVars);
        }
    }
}

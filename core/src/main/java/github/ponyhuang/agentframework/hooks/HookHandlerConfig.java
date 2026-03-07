package github.ponyhuang.agentframework.hooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration for a single hook handler.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HookHandlerConfig {

    @JsonProperty("type")
    private String type;

    @JsonProperty("command")
    private String command;

    @JsonProperty("url")
    private String url;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("timeout")
    private Long timeout;

    @JsonProperty("async")
    private Boolean async = false;

    @JsonProperty("once")
    private Boolean once = false;

    @JsonProperty("statusMessage")
    private String statusMessage;

    @JsonProperty("headers")
    private Map<String, String> headers;

    @JsonProperty("allowedEnvVars")
    private List<String> allowedEnvVars;

    @JsonProperty("model")
    private String model;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public Boolean getOnce() {
        return once;
    }

    public void setOnce(Boolean once) {
        this.once = once;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public List<String> getAllowedEnvVars() {
        return allowedEnvVars;
    }

    public void setAllowedEnvVars(List<String> allowedEnvVars) {
        this.allowedEnvVars = allowedEnvVars;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Converts this config to a HookHandler.
     */
    public HookHandler toHandler() {
        Duration timeoutDuration = timeout != null ?
                Duration.ofSeconds(timeout) : Duration.ofSeconds(600);

        switch (type != null ? type.toLowerCase() : "") {
            case "command":
                return CommandHookHandler.builder()
                        .command(command)
                        .timeout(timeoutDuration)
                        .async(async != null ? async : false)
                        .once(once != null ? once : false)
                        .statusMessage(statusMessage)
                        .build();

            case "http":
                return HttpHookHandler.builder()
                        .url(url)
                        .headers(headers)
                        .timeout(timeoutDuration)
                        .async(async != null ? async : false)
                        .once(once != null ? once : false)
                        .allowedEnvVars(allowedEnvVars != null ?
                                new java.util.HashSet<>(allowedEnvVars) : null)
                        .build();

            case "prompt":
                // Note: PromptHookHandler requires a ChatClient which must be injected separately
                // This is a placeholder - actual implementation would need the ChatClient
                return null;

            default:
                return null;
        }
    }
}

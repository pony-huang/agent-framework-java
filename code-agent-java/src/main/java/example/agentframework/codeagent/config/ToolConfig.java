package example.agentframework.codeagent.config;

import java.util.Map;

/**
 * Configuration for tool selection and customization.
 */
public class ToolConfig {

    private String name;
    private boolean enabled = true;
    private Map<String, Object> options;

    public ToolConfig() {
    }

    public ToolConfig(String name) {
        this.name = name;
    }

    public ToolConfig(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }
}
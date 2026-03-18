package github.ponyhuang.agentframework.agents;

import java.util.Collections;
import java.util.Set;

/**
 * Defines a custom sub-agent that can be used within the main agent.
 * Similar to Python SDK's AgentDefinition.
 */
public class AgentDefinition {

    private final String name;
    private final String description;
    private final String prompt;
    private final Set<String> tools;
    private final String model;

    public AgentDefinition(String name, String description, String prompt, Set<String> tools, String model) {
        this.name = name;
        this.description = description;
        this.prompt = prompt;
        this.tools = tools != null ? Collections.unmodifiableSet(tools) : Collections.emptySet();
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrompt() {
        return prompt;
    }

    public Set<String> getTools() {
        return tools;
    }

    public String getModel() {
        return model;
    }

    /**
     * Create a new AgentDefinition builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String description;
        private String prompt;
        private Set<String> tools;
        private String model;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder tools(Set<String> tools) {
            this.tools = tools;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public AgentDefinition build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Agent name is required");
            }
            if (prompt == null || prompt.isEmpty()) {
                throw new IllegalArgumentException("Agent prompt is required");
            }
            return new AgentDefinition(name, description, prompt, tools, model);
        }
    }
}

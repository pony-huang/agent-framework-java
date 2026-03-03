package github.ponyhuang.agentframework.a2a.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class AgentCard {
    @JsonProperty("name")
    private final String name;
    @JsonProperty("description")
    private final String description;
    @JsonProperty("url")
    private final String url;
    @JsonProperty("version")
    private final String version;
    @JsonProperty("capabilities")
    private final AgentCapabilities capabilities;
    @JsonProperty("supportedProtocolVersions")
    private final List<String> supportedProtocolVersions;
    @JsonProperty("skills")
    private final Map<String, Object> skills;
    @JsonProperty("additionalProperties")
    private final Map<String, Object> additionalProperties;

    @JsonCreator
    public AgentCard(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("url") String url,
            @JsonProperty("version") String version,
            @JsonProperty("capabilities") AgentCapabilities capabilities,
            @JsonProperty("supportedProtocolVersions") List<String> supportedProtocolVersions,
            @JsonProperty("skills") Map<String, Object> skills,
            @JsonProperty("additionalProperties") Map<String, Object> additionalProperties) {
        this.name = name;
        this.description = description;
        this.url = url;
        this.version = version;
        this.capabilities = capabilities;
        this.supportedProtocolVersions = supportedProtocolVersions;
        this.skills = skills;
        this.additionalProperties = additionalProperties;
    }

    public AgentCard(String name, String url) {
        this(name, null, url, null, null, null, null, null);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getVersion() {
        return version;
    }

    public AgentCapabilities getCapabilities() {
        return capabilities;
    }

    public List<String> getSupportedProtocolVersions() {
        return supportedProtocolVersions;
    }

    public Map<String, Object> getSkills() {
        return skills;
    }

    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public boolean supportsStreaming() {
        return capabilities != null && Boolean.TRUE.equals(capabilities.getStreaming());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AgentCard createMinimal(String url) {
        return new AgentCard("Unknown Agent", null, url, "1.0",
                AgentCapabilities.builder().streaming(true).build(), null, null, null);
    }

    public static class Builder {
        private String name;
        private String description;
        private String url;
        private String version;
        private AgentCapabilities capabilities;
        private List<String> supportedProtocolVersions;
        private Map<String, Object> skills;
        private Map<String, Object> additionalProperties;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder capabilities(AgentCapabilities capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder supportedProtocolVersions(List<String> supportedProtocolVersions) {
            this.supportedProtocolVersions = supportedProtocolVersions;
            return this;
        }

        public Builder skills(Map<String, Object> skills) {
            this.skills = skills;
            return this;
        }

        public Builder additionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public AgentCard build() {
            return new AgentCard(name, description, url, version, capabilities,
                    supportedProtocolVersions, skills, additionalProperties);
        }
    }
}

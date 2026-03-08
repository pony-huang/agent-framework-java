package example.agentframework.codeagent.config;

/**
 * Configuration for different LLM providers.
 * Supports OpenAI, Anthropic, and other providers.
 */
public class ModelProviderConfig {

    private String type; // "openai", "anthropic", "google", etc.
    private String model;
    private String apiKey;
    private String baseUrl;

    // Provider-specific options
    private Double temperature;
    private Integer maxTokens;
    private Integer topP;

    // Google-specific
    private String project;
    private String location;

    public ModelProviderConfig() {
    }

    public ModelProviderConfig(String type, String model, String apiKey) {
        this.type = type;
        this.model = model;
        this.apiKey = apiKey;
    }

    // Getters and setters

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getTopP() {
        return topP;
    }

    public void setTopP(Integer topP) {
        this.topP = topP;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
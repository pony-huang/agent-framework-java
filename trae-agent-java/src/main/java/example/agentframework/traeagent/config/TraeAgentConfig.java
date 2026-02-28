package example.agentframework.traeagent.config;

import java.util.List;
import java.util.Map;

/**
 * Main configuration class for TraeAgent.
 * Loaded from YAML configuration file.
 */
public class TraeAgentConfig {

    private String provider = "anthropic";
    private String model = "claude-sonnet-4-20250514";
    private String apiKey = "";
    private String baseUrl = "";

    private List<String> tools = List.of("bash", "edit", "json_edit", "sequential_thinking", "task_done");

    private int maxSteps = 100;
    private String workingDirectory = ".";

    private ModelProviderConfig modelProvider;
    private List<ToolConfig> toolConfigs;
    private List<MCPServerConfig> mcpServers;

    private boolean trajectoryEnabled = true;
    private String trajectoryPath = "./trajectory";

    private Map<String, String> extraArgs;

    // Getters and setters

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
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

    public List<String> getTools() {
        return tools;
    }

    public void setTools(List<String> tools) {
        this.tools = tools;
    }

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public ModelProviderConfig getModelProvider() {
        return modelProvider;
    }

    public void setModelProvider(ModelProviderConfig modelProvider) {
        this.modelProvider = modelProvider;
    }

    public List<ToolConfig> getToolConfigs() {
        return toolConfigs;
    }

    public void setToolConfigs(List<ToolConfig> toolConfigs) {
        this.toolConfigs = toolConfigs;
    }

    public List<MCPServerConfig> getMcpServers() {
        return mcpServers;
    }

    public void setMcpServers(List<MCPServerConfig> mcpServers) {
        this.mcpServers = mcpServers;
    }

    public boolean isTrajectoryEnabled() {
        return trajectoryEnabled;
    }

    public void setTrajectoryEnabled(boolean trajectoryEnabled) {
        this.trajectoryEnabled = trajectoryEnabled;
    }

    public String getTrajectoryPath() {
        return trajectoryPath;
    }

    public void setTrajectoryPath(String trajectoryPath) {
        this.trajectoryPath = trajectoryPath;
    }

    public Map<String, String> getExtraArgs() {
        return extraArgs;
    }

    public void setExtraArgs(Map<String, String> extraArgs) {
        this.extraArgs = extraArgs;
    }
}
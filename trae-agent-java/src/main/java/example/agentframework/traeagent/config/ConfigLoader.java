package example.agentframework.traeagent.config;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Loads and parses YAML configuration for TraeAgent.
 * Supports environment variable overrides.
 */
public class ConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "trae-config.yaml";
    private static final String ENV_PREFIX = "TRAE_";

    private final Yaml yaml;

    public ConfigLoader() {
        this.yaml = new Yaml();
    }

    /**
     * Load configuration from default location.
     */
    public TraeAgentConfig load() throws IOException {
        return load(DEFAULT_CONFIG_FILE);
    }

    /**
     * Load configuration from specified file path.
     */
    public TraeAgentConfig load(String configPath) throws IOException {
        Path path = Paths.get(configPath);
        if (!Files.exists(path)) {
            // Try current working directory
            path = Paths.get(System.getProperty("user.dir"), configPath);
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Config file not found: " + configPath);
            }
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            TraeAgentConfig config = yaml.loadAs(inputStream, TraeAgentConfig.class);
            applyEnvironmentOverrides(config);
            return config;
        }
    }

    /**
     * Load configuration from input stream.
     */
    public TraeAgentConfig load(InputStream inputStream) {
        TraeAgentConfig config = yaml.loadAs(inputStream, TraeAgentConfig.class);
        applyEnvironmentOverrides(config);
        return config;
    }

    /**
     * Apply environment variable overrides.
     * Supports: TRAE_PROVIDER, TRAE_MODEL, TRAE_API_KEY, TRAE_BASE_URL, TRAE_MAX_STEPS, etc.
     */
    private void applyEnvironmentOverrides(TraeAgentConfig config) {
        // Provider
        Optional.ofNullable(System.getenv(ENV_PREFIX + "PROVIDER"))
                .or(() -> Optional.ofNullable(System.getenv("ANTHROPIC_PROVIDER")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_PROVIDER")))
                .ifPresent(config::setProvider);

        // Model
        Optional.ofNullable(System.getenv(ENV_PREFIX + "MODEL"))
                .or(() -> Optional.ofNullable(System.getenv("ANTHROPIC_MODEL")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_MODEL")))
                .ifPresent(config::setModel);

        // API Key - multiple sources
        Optional.ofNullable(System.getenv(ENV_PREFIX + "API_KEY"))
                .or(() -> Optional.ofNullable(System.getenv("ANTHROPIC_API_KEY")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_API_KEY")))
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_KEY")))
                .ifPresent(config::setApiKey);

        // Base URL
        Optional.ofNullable(System.getenv(ENV_PREFIX + "BASE_URL"))
                .ifPresent(config::setBaseUrl);

        // Max Steps
        Optional.ofNullable(System.getenv(ENV_PREFIX + "MAX_STEPS"))
                .map(Integer::parseInt)
                .ifPresent(config::setMaxSteps);

        // Working Directory
        Optional.ofNullable(System.getenv(ENV_PREFIX + "WORKING_DIRECTORY"))
                .or(() -> Optional.ofNullable(System.getenv("WORKING_DIR")))
                .ifPresent(config::setWorkingDirectory);

        // Trajectory
        Optional.ofNullable(System.getenv(ENV_PREFIX + "TRAJECTORY_ENABLED"))
                .map(Boolean::parseBoolean)
                .ifPresent(config::setTrajectoryEnabled);

        Optional.ofNullable(System.getenv(ENV_PREFIX + "TRAJECTORY_PATH"))
                .ifPresent(config::setTrajectoryPath);
    }

    /**
     * Create configuration programmatically with required fields.
     */
    public static TraeAgentConfig create(String provider, String model, String apiKey) {
        TraeAgentConfig config = new TraeAgentConfig();
        config.setProvider(provider);
        config.setModel(model);
        config.setApiKey(apiKey);
        return config;
    }

    /**
     * Create configuration with builder pattern.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TraeAgentConfig config = new TraeAgentConfig();

        public Builder provider(String provider) {
            config.setProvider(provider);
            return this;
        }

        public Builder model(String model) {
            config.setModel(model);
            return this;
        }

        public Builder apiKey(String apiKey) {
            config.setApiKey(apiKey);
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            config.setBaseUrl(baseUrl);
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            config.setMaxSteps(maxSteps);
            return this;
        }

        public Builder workingDirectory(String workingDirectory) {
            config.setWorkingDirectory(workingDirectory);
            return this;
        }

        public Builder tools(String... tools) {
            config.setTools(java.util.Arrays.asList(tools));
            return this;
        }

        public Builder trajectoryEnabled(boolean enabled) {
            config.setTrajectoryEnabled(enabled);
            return this;
        }

        public Builder trajectoryPath(String path) {
            config.setTrajectoryPath(path);
            return this;
        }

        public TraeAgentConfig build() {
            // Apply env overrides
            ConfigLoader loader = new ConfigLoader();
            loader.applyEnvironmentOverrides(config);
            return config;
        }
    }
}
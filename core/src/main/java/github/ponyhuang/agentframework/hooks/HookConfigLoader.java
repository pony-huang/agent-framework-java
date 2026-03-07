package github.ponyhuang.agentframework.hooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Loads hook configurations from JSON files.
 */
public class HookConfigLoader {

    private static final Logger LOG = LoggerFactory.getLogger(HookConfigLoader.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Loads hook configuration from a file.
     *
     * @param filePath the path to the settings.json file
     * @return the hook config, or empty if not found
     */
    public static Optional<HookConfig> loadFromFile(String filePath) {
        return loadFromFile(new File(filePath));
    }

    /**
     * Loads hook configuration from a File.
     *
     * @param file the settings.json file
     * @return the hook config, or empty if not found
     */
    public static Optional<HookConfig> loadFromFile(File file) {
        if (!file.exists()) {
            LOG.debug("Hook config file not found: {}", file.getPath());
            return Optional.empty();
        }

        try {
            String content = Files.readString(file.toPath());
            return loadFromJson(content);
        } catch (IOException e) {
            LOG.error("Failed to read hook config file: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Loads hook configuration from JSON string.
     *
     * @param json the JSON content
     * @return the hook config, or empty if parsing fails
     */
    public static Optional<HookConfig> loadFromJson(String json) {
        try {
            HookConfig config = MAPPER.readValue(json, HookConfig.class);
            return Optional.of(config);
        } catch (IOException e) {
            LOG.error("Failed to parse hook config JSON: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Loads hook configuration from standard locations.
     * Searches in order: local, project, user
     *
     * @param projectDir the project directory (optional)
     * @return the merged hook config
     */
    public static HookConfig loadFromStandardLocations(String projectDir) {
        Map<HookEvent, java.util.List<HookExecutor.HookRegistration>> merged = new HashMap<>();

        // User settings: ~/.claude/settings.json
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            loadAndMerge(new File(userHome, ".claude/settings.json"), merged);
        }

        // Project settings: .claude/settings.json
        if (projectDir != null) {
            loadAndMerge(new File(projectDir, ".claude/settings.json"), merged);
        }

        // Local settings: .claude/settings.local.json (highest priority)
        if (projectDir != null) {
            loadAndMerge(new File(projectDir, ".claude/settings.local.json"), merged);
        }

        // Build final config
        Map<String, java.util.List<HookMatcherGroup>> hooksMap = new HashMap<>();
        for (Map.Entry<HookEvent, java.util.List<HookExecutor.HookRegistration>> entry : merged.entrySet()) {
            hooksMap.put(entry.getKey().getEventName(), new java.util.ArrayList<>());
        }

        HookConfig config = new HookConfig();
        config.setHooks(hooksMap);
        return config;
    }

    private static void loadAndMerge(File file, Map<HookEvent, java.util.List<HookExecutor.HookRegistration>> merged) {
        Optional<HookConfig> optConfig = loadFromFile(file);
        if (optConfig.isPresent()) {
            HookConfig config = optConfig.get();
            Map<HookEvent, java.util.List<HookExecutor.HookRegistration>> registrations = config.toRegistrationMap();

            for (Map.Entry<HookEvent, java.util.List<HookExecutor.HookRegistration>> entry : registrations.entrySet()) {
                merged.computeIfAbsent(entry.getKey(), k -> new java.util.ArrayList<>())
                        .addAll(entry.getValue());
            }

            LOG.info("Loaded hooks from: {}", file.getPath());
        }
    }
}

package github.ponyhuang.agentframework.samples.hooks;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.hooks.HookConfig;
import github.ponyhuang.agentframework.hooks.HookConfigLoader;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.providers.AnthropicChatClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Example demonstrating how to load hooks from JSON configuration.
 *
 * This example shows:
 * 1. JSON configuration format
 * 2. Loading hooks from file
 * 3. Using settings.local.json
 */
public class HookConfigExample {

    public static void main(String[] args) throws Exception {
        // Example 1: JSON configuration format
        exampleJsonFormat();

        // Example 2: Loading from file
        exampleLoadFromFile();

        // Example 3: Loading from standard locations
        exampleLoadFromStandardLocations();
    }

    /**
     * Example 1: Show JSON configuration format
     */
    static void exampleJsonFormat() {
        System.out.println("=== Example 1: JSON Configuration Format ===\n");

        String json = """
                {
                  "hooks": {
                    "SessionStart": [
                      {
                        "matcher": ".*",
                        "hooks": [
                          {
                            "type": "command",
                            "command": "/path/to/script.sh",
                            "timeout": 60,
                            "async": false,
                            "statusMessage": "Running startup hook..."
                          }
                        ]
                      }
                    ],
                    "PreToolUse": [
                      {
                        "matcher": "Bash",
                        "hooks": [
                          {
                            "type": "command",
                            "command": "/usr/local/bin/validate-tool.sh",
                            "timeout": 30,
                            "async": false
                          }
                        ]
                      },
                      {
                        "matcher": "Read|Write",
                        "hooks": [
                          {
                            "type": "http",
                            "url": "http://localhost:8080/hooks/pre-tool",
                            "timeout": 30,
                            "headers": {
                              "Authorization": "Bearer $API_TOKEN"
                            },
                            "allowedEnvVars": ["API_TOKEN"]
                          }
                        ]
                      }
                    ],
                    "PostToolUse": [
                      {
                        "matcher": ".*",
                        "hooks": [
                          {
                            "type": "command",
                            "command": "/usr/local/bin/log-tool.sh",
                            "timeout": 10,
                            "async": true,
                            "once": false
                          }
                        ]
                      }
                    ],
                    "Stop": [
                      {
                        "matcher": ".*",
                        "hooks": [
                          {
                            "type": "command",
                            "command": "/usr/local/bin/cleanup.sh",
                            "timeout": 60,
                            "async": false
                          }
                        ]
                      }
                    ]
                  },
                  "disableAllHooks": false
                }
                """;

        System.out.println("Example settings.json content:\n");
        System.out.println(json);
        System.out.println("\nHook configuration fields:\n");
        System.out.println("  - hooks: Map of event name -> list of matcher groups");
        System.out.println("  - matcher: Regex pattern to match (null/* = match all)");
        System.out.println("  - hooks: List of hook configurations");
        System.out.println("  - type: 'command', 'http', or 'prompt'");
        System.out.println("  - command/url/prompt: Handler-specific configuration");
        System.out.println("  - timeout: Timeout in seconds (default: 600 for command, 30 for http/prompt)");
        System.out.println("  - async: Run hook asynchronously (default: false)");
        System.out.println("  - once: Run hook only once per session (default: false)");
        System.out.println("  - statusMessage: Message to show while hook runs");
        System.out.println("  - headers: HTTP headers (for http type)");
        System.out.println("  - allowedEnvVars: Environment variables allowed in headers");
    }

    /**
     * Example 2: Loading hooks from JSON file
     */
    static void exampleLoadFromFile() throws Exception {
        System.out.println("\n=== Example 2: Load From File ===\n");

        // Create a temporary config file
        String json = """
                {
                  "hooks": {
                    "PreToolUse": [
                      {
                        "matcher": "Bash",
                        "hooks": [
                          {
                            "type": "command",
                            "command": "echo 'PreToolUse hook fired'",
                            "timeout": 10
                          }
                        ]
                      }
                    ]
                  }
                }
                """;

        Path tempFile = Files.createTempFile("hooks", ".json");
        Files.writeString(tempFile, json);

        try {
            // Load configuration
            Optional<HookConfig> configOpt = HookConfigLoader.loadFromFile(tempFile.toFile());

            if (configOpt.isPresent()) {
                HookConfig config = configOpt.get();
                System.out.println("Loaded config:");
                System.out.println("  disableAllHooks: " + config.isDisableAllHooks());
                System.out.println("  hooks: " + config.getHooks().keySet());

                // Convert to registrations
                var registrations = config.toRegistrationMap();
                System.out.println("  registrations: " + registrations.size() + " events");

                // Create executor
                HookExecutor executor = HookExecutor.builder()
                        .hookConfigs(registrations)
                        .build();

                System.out.println("HookExecutor created successfully!");
            }
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    /**
     * Example 3: Loading from standard locations
     */
    @SuppressWarnings("unused")
    static void exampleLoadFromStandardLocations() {
        System.out.println("\n=== Example 3: Standard Locations ===\n");

        System.out.println("Standard hook configuration locations (in order of precedence):");
        System.out.println("  1. ~/.claude/settings.json (user settings)");
        System.out.println("  2. <project>/.claude/settings.json (project settings)");
        System.out.println("  3. <project>/.claude/settings.local.json (local settings, highest priority)");
        System.out.println();

        System.out.println("To load from standard locations:");
        System.out.println("  HookConfig config = HookConfigLoader.loadFromStandardLocations(projectDir);");
        System.out.println();

        // Note: This would look for files in actual locations
        // HookConfig config.loadFromStandardLocations = HookConfigLoader("/path/to/project");
    }

    /**
     * Example 4: Full agent with JSON config
     */
    @SuppressWarnings("unused")
    static void exampleFullAgentWithJsonConfig() throws Exception {
        // Create a ChatClient
        ChatClient client = AnthropicChatClient.builder()
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                .build();

        // Load hook configuration from file
        File configFile = new File(".claude/settings.json");
        Optional<HookConfig> configOpt = HookConfigLoader.loadFromFile(configFile);

        if (configOpt.isEmpty()) {
            System.out.println("No hook configuration found");
            return;
        }

        HookConfig config = configOpt.get();
        var registrations = config.toRegistrationMap();

        // Create hook executor
        HookExecutor hookExecutor = HookExecutor.builder()
                .hookConfigs(registrations)
                .build();

        // Build agent with hooks
        Agent agent = AgentBuilder.builder()
                .client(client)
                .instructions("You are a helpful assistant.")
                .hookExecutor(hookExecutor)
                .build();

        System.out.println("Agent built with hooks from " + configFile.getPath());
    }
}

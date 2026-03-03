package example.agentframework.traeagent.samples;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.event.*;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.providers.OpenAIChatClient;
import github.ponyhuang.agentframework.types.ChatResponse;
import example.agentframework.traeagent.TraeAgent;
import example.agentframework.traeagent.config.TraeAgentConfig;
import example.agentframework.traeagent.tools.TraeToolRegistry;
import example.agentframework.traeagent.trajectory.TrajectoryRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple example showing how to use TraeAgent.
 *
 * This example demonstrates:
 * - Creating a TraeAgent with configuration
 * - Setting up tools
 * - Subscribing to AG-UI events
 * - Executing a simple task
 */
public class SimpleTraeAgentExample {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(SimpleTraeAgentExample.class);

    public static void main(String[] args) throws Exception {
        // Create configuration
        TraeAgentConfig config = new TraeAgentConfig();
        config.setProvider("openai");
        config.setBaseUrl(System.getenv("MY_OPENAI_BASE_URL"));
        config.setModel(System.getenv("MY_OPENAI_MODEL"));
        config.setApiKey(System.getenv("MY_OPENAI_API_KEY"));
        config.setMaxSteps(50);
        config.setWorkingDirectory(".");

        // Create chat client (Anthropic)
        ChatClient client = OpenAIChatClient.builder()
                .apiKey(config.getApiKey())
                .model(config.getModel())
                .baseUrl(config.getBaseUrl())
                .build();

        // Create tool registry
        TraeToolRegistry toolRegistry = new TraeToolRegistry(config);

        // Create trajectory recorder (optional)
        TrajectoryRecorder trajectoryRecorder = new TrajectoryRecorder(
                "./trajectory/trajectory.json", true);

        // Build the agent
        TraeAgent agent = TraeAgent.builder()
                .name("trae-agent")
                .client(client)
                .config(config)
                .toolExecutor(toolRegistry.getToolExecutor())
                .trajectoryRecorder(trajectoryRecorder)
                .build();

        // Add AG-UI event subscriber to print events
        AgentSubscriber eventLogger = new AgentSubscriber() {
            @Override
            public void onEvent(BaseEvent event) {
                try {
                    LOG.info("[AG-UI EVENT] {}: {}", event.getType().getName(), toJson(event));
                } catch (Exception e) {
                    LOG.error("Error serializing event: {}", e.getMessage());
                }
            }

            @Override
            public void onTextMessageContentEvent(TextMessageContentEvent event) {
                LOG.info("[TEXT_MESSAGE_CONTENT] messageId={}, delta=\"{}\"", event.getMessageId(), truncate(event.getDelta(), 100));
            }

            @Override
            public void onToolCallStartEvent(ToolCallStartEvent event) {
                LOG.info("[TOOL_CALL_START] toolCallId={}, toolName={}", event.getToolCallId(), event.getToolCallName());
            }

            @Override
            public void onToolCallResultEvent(ToolCallResultEvent event) {
                LOG.info("[TOOL_CALL_RESULT] toolCallId={}, result=\"{}\"", event.getToolCallId(), truncate(event.getContent(), 200));
            }
        };

        agent.addSubscriber(eventLogger);

        // Execute a simple task
        String task = "Create a simple HelloWorld.java file in the current directory that prints 'Hello, World!'";

        System.out.println("Executing task: " + task);
        System.out.println("---");

        ChatResponse response = agent.newTask(task, java.util.Map.of(
                "project_path", config.getWorkingDirectory()
        ));

        // Print response
        if (response.getMessage() != null) {
            System.out.println(response.getMessage().getText());
        }

        // Check completion status
        System.out.println("---");
        if (agent.isTaskCompleted()) {
            System.out.println("Task completed!");
        } else {
            System.out.println("Task may not be complete");
        }
    }

    private static String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
}
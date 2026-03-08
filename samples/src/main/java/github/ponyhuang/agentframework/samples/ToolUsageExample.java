package github.ponyhuang.agentframework.samples;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.agents.AgentBuilder;
import github.ponyhuang.agentframework.clients.ChatClient;
import github.ponyhuang.agentframework.tools.ToolParam;
import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolExecutor;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.message.ResultMessage;
import github.ponyhuang.agentframework.types.block.Block;
import github.ponyhuang.agentframework.types.block.ToolUseBlock;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Example showing how to use tools with agents.
 */
public class ToolUsageExample {

    public static void main(String[] args) {
        // Create a tool instance
        WeatherTool weatherTool = new WeatherTool();

        // Create a chat client
        ChatClient client = ClientExample.openAIChatClient();

        // Build an agent with tools using simplified API
        // Note: ToolExecutor is created internally, use getToolExecutor() if needed for manual execution
        AgentBuilder agentBuilder = AgentBuilder.builder()
                .name("assistant")
                .instructions("You are a helpful assistant. For weather questions, you must call the get_weather tool.")
                .client(client)
                .tool(weatherTool);

        // Get tool schema for demonstration
        Map<String, Object> toolSchema = agentBuilder.getToolExecutor().getToolSchemas().get(0);
        System.out.println("Tool Schema: " + toolSchema);

        // Build the agent
        Agent agent = agentBuilder.build();

        // Run the agent with a question that needs a tool
        List<Message> messages = new ArrayList<>();
        messages.add(UserMessage.create("What's the weather like in Tokyo?"));

        // Use the internal tool executor for manual tool loop
        ToolExecutor executor = agentBuilder.getToolExecutor();
        ChatResponse response = runWithTools(agent, executor, messages, 3);
        System.out.println("Response: " + response.getMessage().getTextContent());
    }

    /**
     * Example tool class for weather queries.
     */
    public static class WeatherTool {

        @Tool(
                name = "get_weather",
                description = "Get the current weather for a city"
        )
        public String getWeather(
                @ToolParam(description = "The city name") String city
        ) {
            // In real implementation, call a weather API
            return "The weather in " + city + " is sunny, 22°C";
        }
    }

    private static ChatResponse runWithTools(Agent agent,
                                             ToolExecutor executor,
                                             List<Message> messages,
                                             int maxRounds) {
        ChatResponse response = null;
        for (int i = 0; i < maxRounds; i++) {
            List<Message> collected = agent.runStream(messages).collectList().block();
            response = ChatResponse.builder().messages(collected).build();
            Message assistant = response.getMessage();
            if (assistant == null) {
                break;
            }

            messages.add(assistant);
            List<Map<String, Object>> toolCalls = extractToolCalls(assistant);
            if (toolCalls.isEmpty()) {
                break;
            }

            for (Map<String, Object> call : toolCalls) {
                String name = (String) call.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> args = (Map<String, Object>) call.get("arguments");
                if (args == null) {
                    args = new HashMap<>();
                }
                String toolCallId = (String) call.get("id");
                if (toolCallId == null || toolCallId.isBlank()) {
                    toolCallId = "tool_" + UUID.randomUUID();
                }
                Object result = executor.execute(name, args);
                messages.add(ResultMessage.create(toolCallId, name, result));
            }
        }
        return response;
    }

    private static List<Map<String, Object>> extractToolCalls(Message message) {
        List<Map<String, Object>> calls = new ArrayList<>();
        if (message.getBlocks() == null) {
            return calls;
        }
        for (Block block : message.getBlocks()) {
            if (block instanceof ToolUseBlock) {
                ToolUseBlock toolUse = (ToolUseBlock) block;
                Map<String, Object> call = new HashMap<>();
                call.put("id", toolUse.getId());
                call.put("name", toolUse.getName());
                call.put("arguments", toolUse.getInput());
                calls.add(call);
            }
        }
        return calls;
    }
}

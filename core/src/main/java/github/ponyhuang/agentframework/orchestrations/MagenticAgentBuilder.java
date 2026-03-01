package github.ponyhuang.agentframework.orchestrations;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Message;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Builder for Magentic One agent pattern.
 * One orchestrator agent coordinates multiple specialist agents.
 */
public class MagenticAgentBuilder {

    private Agent orchestrator;
    private final Map<String, Agent> specialists = new HashMap<>();
    private int maxIterations = 10;

    /**
     * Sets the orchestrator agent.
     *
     * @param orchestrator the orchestrator agent
     * @return this builder
     */
    public MagenticAgentBuilder orchestrator(Agent orchestrator) {
        this.orchestrator = orchestrator;
        return this;
    }

    /**
     * Adds a specialist agent.
     *
     * @param name     the specialist name
     * @param specialist the specialist agent
     * @return this builder
     */
    public MagenticAgentBuilder specialist(String name, Agent specialist) {
        specialists.put(name, specialist);
        return this;
    }

    /**
     * Sets the maximum number of iterations.
     *
     * @param maxIterations the maximum iterations
     * @return this builder
     */
    public MagenticAgentBuilder maxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    /**
     * Executes the Magentic One pattern.
     *
     * @param task the task description
     * @return the final result
     */
    public ChatResponse execute(String task) {
        if (orchestrator == null) {
            throw new IllegalStateException("Orchestrator is required");
        }

        List<Message> conversation = new ArrayList<>();
        conversation.add(Message.user(task));

        String lastSpecialistResult = null;

        for (int i = 0; i < maxIterations; i++) {
            // Orchestrator decides next action
            ChatResponse orchestratorResponse = orchestrator.run(new ArrayList<>(conversation));

            if (orchestratorResponse.getMessage() == null) {
                break;
            }

            conversation.add(orchestratorResponse.getMessage());

            // Check if orchestrator is calling a specialist
            String responseText = orchestratorResponse.getMessage().getText();

            // Look for specialist call in response
            String specialistName = extractSpecialistCall(responseText);
            if (specialistName != null) {
                Agent specialist = specialists.get(specialistName);
                if (specialist != null) {
                    // Execute specialist
                    String specialistTask = extractSpecialistTask(responseText);
                    ChatResponse specialistResponse = specialist.run(
                            List.of(Message.user(specialistTask != null ? specialistTask : ""))
                    );

                    if (specialistResponse.getMessage() != null) {
                        lastSpecialistResult = specialistResponse.getMessage().getText();
                        conversation.add(specialistResponse.getMessage());
                    }
                }
            } else if (isTaskComplete(responseText)) {
                // Task is complete
                return orchestratorResponse;
            }
        }

        // Return final response
        return conversation.size() > 0
                ? createResponseFromMessage(conversation.get(conversation.size() - 1))
                : null;
    }

    private String extractSpecialistCall(String response) {
        // Look for patterns like "[SPECIALIST_NAME]" or "call specialist: name"
        if (response == null) return null;

        for (String specialistName : specialists.keySet()) {
            if (response.contains("[" + specialistName + "]") ||
                    response.toLowerCase().contains("call " + specialistName.toLowerCase())) {
                return specialistName;
            }
        }
        return null;
    }

    private String extractSpecialistTask(String response) {
        // Extract task description after specialist call
        // This is a simplified implementation
        return response;
    }

    private boolean isTaskComplete(String response) {
        if (response == null) return false;
        String lower = response.toLowerCase();
        return lower.contains("complete") ||
                lower.contains("finished") ||
                lower.contains("done") ||
                lower.contains("final answer");
    }

    private ChatResponse createResponseFromMessage(Message message) {
        return ChatResponse.builder()
                .choices(List.of(new ChatResponse.Choice(0, message, "stop")))
                .build();
    }

    /**
     * Executes specialists in parallel when orchestrator requests multiple.
     *
     * @param tasks map of specialist names to tasks
     * @return map of specialist names to results
     */
    public Map<String, String> executeSpecialists(Map<String, String> tasks) {
        List<CompletableFuture<Map.Entry<String, String>>> futures = tasks.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> {
                    Agent specialist = specialists.get(entry.getKey());
                    if (specialist == null) {
                        return Map.entry(entry.getKey(), "Unknown specialist");
                    }

                    ChatResponse response = specialist.run(
                            List.of(Message.user(entry.getValue()))
                    );

                    String result = response.getMessage() != null
                            ? response.getMessage().getText()
                            : "No response";
                    return Map.entry(entry.getKey(), result);
                }))
                .collect(Collectors.toList());

        Map<String, String> results = new HashMap<>();
        for (CompletableFuture<Map.Entry<String, String>> future : futures) {
            try {
                Map.Entry<String, String> entry = future.get();
                results.put(entry.getKey(), entry.getValue());
            } catch (InterruptedException | ExecutionException e) {
                // Skip failed specialist
            }
        }
        return results;
    }

    /**
     * Gets the number of specialists.
     *
     * @return the specialist count
     */
    public int getSpecialistCount() {
        return specialists.size();
    }
}

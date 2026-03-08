package github.ponyhuang.agentframework.orchestrations;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.hooks.HookExecutor;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.events.SubagentStartContext;
import github.ponyhuang.agentframework.hooks.events.SubagentStopContext;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import github.ponyhuang.agentframework.types.block.TextBlock;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Function;

/**
 * Builder for group chat agent execution.
 * Multiple agents participate in a chat, with a moderator coordinating.
 */
public class GroupChatAgentBuilder {

    private final List<Agent> participants = new ArrayList<>();
    private Agent moderator;
    private int maxTurns = 10;
    private Function<List<Message>, Agent> selectionStrategy;
    private HookExecutor hookExecutor;

    /**
     * Sets the hook executor.
     *
     * @param hookExecutor the hook executor
     * @return this builder
     */
    public GroupChatAgentBuilder hookExecutor(HookExecutor hookExecutor) {
        this.hookExecutor = hookExecutor;
        return this;
    }

    /**
     * Adds a participant to the group chat.
     *
     * @param agent the participant agent
     * @return this builder
     */
    public GroupChatAgentBuilder participant(Agent agent) {
        participants.add(agent);
        return this;
    }

    /**
     * Sets the moderator for the group chat.
     *
     * @param moderator the moderator agent
     * @return this builder
     */
    public GroupChatAgentBuilder moderator(Agent moderator) {
        this.moderator = moderator;
        return this;
    }

    /**
     * Sets the maximum number of turns.
     *
     * @param maxTurns the maximum turns
     * @return this builder
     */
    public GroupChatAgentBuilder maxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
        return this;
    }

    /**
     * Sets the agent selection strategy.
     *
     * @param strategy function that selects next agent
     * @return this builder
     */
    public GroupChatAgentBuilder selectionStrategy(Function<List<Message>, Agent> strategy) {
        this.selectionStrategy = strategy;
        return this;
    }

    /**
     * Executes the group chat.
     *
     * @param input the initial input
     * @return the final response from moderator
     */
    public ChatResponse execute(String input) {
        return execute(input, null);
    }

    /**
     * Executes the group chat with initial messages.
     *
     * @param input the initial input
     * @param initialMessages initial messages
     * @return the final response from moderator
     */
    public ChatResponse execute(String input, List<Message> initialMessages) {
        if (participants.isEmpty()) {
            throw new IllegalStateException("No participants in group chat");
        }

        List<Message> conversationHistory = new ArrayList<>();

        // Add initial messages
        if (initialMessages != null) {
            conversationHistory.addAll(initialMessages);
        }

        // Add user message
        conversationHistory.add(UserMessage.create(input));

        Agent currentSpeaker = selectionStrategy != null
                ? selectionStrategy.apply(conversationHistory)
                : participants.get(0);

        for (int turn = 0; turn < maxTurns; turn++) {
            // Determine hook executor for this agent
            HookExecutor executor = hookExecutor != null ? hookExecutor : currentSpeaker.getHookExecutor();
            String subagentId = UUID.randomUUID().toString();

            // Fire SubagentStart
            if (executor != null) {
                SubagentStartContext startContext = new SubagentStartContext();
                startContext.setAgentId(currentSpeaker.getName());
                startContext.setAgentType("group_chat_participant");
                startContext.setCwd(System.getProperty("user.dir"));
                startContext.setPermissionMode("default");
                HookResult result = executor.executeSubagentStart(startContext);
                if (!result.isAllow()) {
                    System.out.println("SubagentStart hook blocked agent: " + currentSpeaker.getName());
                    // Skip this turn or handle blocking
                    // For now, let's break or skip. Let's select next speaker and continue.
                    if (selectionStrategy != null) {
                        currentSpeaker = selectionStrategy.apply(conversationHistory);
                    } else {
                        int currentIndex = participants.indexOf(currentSpeaker);
                        currentSpeaker = participants.get((currentIndex + 1) % participants.size());
                    }
                    continue;
                }
            }

            // Current speaker responds
            List<Message> responseMessages = currentSpeaker.runStream(new ArrayList<>(conversationHistory)).collectList().block();
            ChatResponse response = ChatResponse.builder()
                    .messages(responseMessages)
                    .build();

            // Fire SubagentStop
            if (executor != null) {
                SubagentStopContext stopContext = new SubagentStopContext();
                stopContext.setAgentId(currentSpeaker.getName());
                stopContext.setAgentType("group_chat_participant");
                if (response.getMessage() != null) {
                    stopContext.setLastAssistantMessage(getMessageText(response.getMessage()));
                }
                stopContext.setCwd(System.getProperty("user.dir"));
                stopContext.setPermissionMode("default");
                executor.executeSubagentStop(stopContext);
            }

            if (response.getMessage() != null) {
                conversationHistory.add(response.getMessage());

                // Check if moderator should end the conversation
                if (moderator != null) {
                    List<Message> moderatorMessages = moderator.runStream(new ArrayList<>(conversationHistory)).collectList().block();
                    ChatResponse moderatorResponse = ChatResponse.builder()
                            .messages(moderatorMessages)
                            .build();
                    if (shouldEndConversation(moderatorResponse)) {
                        return moderatorResponse;
                    }
                }
            }

            // Select next speaker
            if (selectionStrategy != null) {
                currentSpeaker = selectionStrategy.apply(conversationHistory);
            } else {
                int currentIndex = participants.indexOf(currentSpeaker);
                currentSpeaker = participants.get((currentIndex + 1) % participants.size());
            }
        }

        // Return last response
        return conversationHistory.size() > 0 && "assistant".equalsIgnoreCase(conversationHistory.get(conversationHistory.size() - 1).getRoleAsString())
                ? createResponseFromMessage(conversationHistory.get(conversationHistory.size() - 1))
                : null;
    }

    private boolean shouldEndConversation(ChatResponse moderatorResponse) {
        if (moderatorResponse == null || moderatorResponse.getMessage() == null) {
            return false;
        }
        String text = getMessageText(moderatorResponse.getMessage());
        return text != null && (text.toLowerCase().contains("end") || text.toLowerCase().contains("conclusion") || text.toLowerCase().contains("finished"));
    }

    private String getMessageText(Message message) {
        if (message.getBlocks() == null) return null;
        StringBuilder sb = new StringBuilder();
        for (github.ponyhuang.agentframework.types.block.Block block : message.getBlocks()) {
            if (block instanceof TextBlock) {
                sb.append(((TextBlock) block).getText());
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private ChatResponse createResponseFromMessage(Message message) {
        return ChatResponse.builder()
                .messages(List.of(message))
                .build();
    }

    /**
     * Creates a round-robin selection strategy.
     *
     * @return a selection strategy function
     */
    public static Function<List<Message>, Agent> roundRobinStrategy(List<Agent> participants) {
        int[] index = {0};
        return messages -> {
            Agent agent = participants.get(index[0] % participants.size());
            index[0]++;
            return agent;
        };
    }

    /**
     * Creates a random selection strategy.
     *
     * @param participants the participants
     * @return a selection strategy function
     */
    public static Function<List<Message>, Agent> randomStrategy(List<Agent> participants) {
        Random random = new Random();
        return messages -> participants.get(random.nextInt(participants.size()));
    }

    /**
     * Gets the number of participants.
     *
     * @return the participant count
     */
    public int getParticipantCount() {
        return participants.size();
    }
}

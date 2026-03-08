//package github.ponyhuang.agentframework.agui.agent;
//
//import github.ponyhuang.agentframework.agents.Agent;
//import github.ponyhuang.agentframework.clients.ChatClient;
//import github.ponyhuang.agentframework.sessions.AgentSession;
//import github.ponyhuang.agentframework.sessions.ContextProvider;
//import github.ponyhuang.agentframework.tools.ToolExecutor;
//import github.ponyhuang.agentframework.types.ChatResponse;
//import github.ponyhuang.agentframework.types.message.Message;
//import reactor.core.publisher.Flux;
//
//import java.util.*;
//
//public class AguiAgentSession implements AgentSession {
//
//    private final Agent agent;
//    private final ChatClient client;
//    private final ToolExecutor toolExecutor;
//    private final Map<String, Object> options;
//    private final String id;
//    private final List<Message> messages = new ArrayList<>();
//    private final Map<String, Object> metadata = new HashMap<>();
//
//    public AguiAgentSession(Agent agent, ChatClient client, ToolExecutor toolExecutor, Map<String, Object> options) {
//        this.agent = agent;
//        this.client = client;
//        this.toolExecutor = toolExecutor;
//        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
//        this.id = UUID.randomUUID().toString();
//    }
//
//    @Override
//    public Agent getAgent() {
//        return agent;
//    }
//
//    @Override
//    public String getId() {
//        return id;
//    }
//
//    @Override
//    public List<Message> getMessages() {
//        return new ArrayList<>(messages);
//    }
//
//    @Override
//    public List<Message> getHistory(int limit) {
//        if (limit <= 0) {
//            return new ArrayList<>(messages);
//        }
//        int size = messages.size();
//        return new ArrayList<>(messages.subList(Math.max(0, size - limit), size));
//    }
//
//    @Override
//    public void addMessage(Message message) {
//        if (message != null) {
//            messages.add(message);
//        }
//    }
//
//    @Override
//    public void addMessages(List<Message> messages) {
//        if (messages != null) {
//            this.messages.addAll(messages);
//        }
//    }
//
//    @Override
//    public ChatResponse run(Message input) {
//        if (input != null) {
//            messages.add(input);
//        }
//
//        List<Message> conversationMessages = new ArrayList<>(messages);
//        int maxSteps = (int) options.getOrDefault("maxSteps", 10);
//        int currentStep = 0;
//
//        while (currentStep < maxSteps) {
//            currentStep++;
//
//            github.ponyhuang.agentframework.types.ChatCompleteParams params =
//                    github.ponyhuang.agentframework.types.ChatCompleteParams.builder()
//                            .messages(conversationMessages)
//                            .tools(toolExecutor != null ? toolExecutor.getToolSchemas() : List.of())
//                            .build();
//
//            ChatResponse response = client.chat(params);
//
//            Message assistantMessage = response.getMessage();
//            conversationMessages.add(assistantMessage);
//            messages.add(assistantMessage);
//
//            if (!response.hasFunctionCall()) {
//                return response;
//            }
//
//            Map<String, Object> functionCall = assistantMessage.getBlocks().getLast();
//            if (functionCall == null) {
//                break;
//            }
//
//            String functionName = (String) functionCall.get("name");
//            @SuppressWarnings("unchecked")
//            Map<String, Object> functionArgs = (Map<String, Object>) functionCall.get("arguments");
//            String toolCallId = (String) functionCall.get("id");
//
//            Object toolResult;
//            try {
//                if (toolExecutor != null) {
//                    toolResult = toolExecutor.execute(functionName,
//                            functionArgs != null ? functionArgs : Collections.emptyMap());
//                } else {
//                    toolResult = "Error: No tool executor configured";
//                }
//            } catch (Exception e) {
//                toolResult = "Error: " + e.getMessage();
//            }
//
//            String resultStr = toolResult != null ? toolResult.toString() : "null";
//            Message toolMessage = Message.tool(toolCallId, functionName, resultStr);
//            conversationMessages.add(toolMessage);
//            messages.add(toolMessage);
//
//            if ("task_done".equals(functionName)) {
//                return response;
//            }
//        }
//
//        return ChatResponse.builder()
//                .choices(List.of(new ChatResponse.Choice(0,
//                        Message.assistant("Maximum steps reached without task completion."),
//                        "max_tokens")))
//                .build();
//    }
//
//    @Override
//    public Flux<Message> runStream(Message input) {
//        return agent.runStream(new ArrayList<>(messages));
//    }
//
//    public void clear() {
//        messages.clear();
//    }
//
//    @Override
//    public void clearHistory() {
//        messages.clear();
//    }
//
//    @Override
//    public Map<String, Object> getMetadata() {
//        return new HashMap<>(metadata);
//    }
//
//    @Override
//    public void setMetadata(String key, Object value) {
//        metadata.put(key, value);
//    }
//
//    @Override
//    public Object getMetadata(String key) {
//        return metadata.get(key);
//    }
//
//    public ContextProvider getContextProvider() {
//        return null;
//    }
//}

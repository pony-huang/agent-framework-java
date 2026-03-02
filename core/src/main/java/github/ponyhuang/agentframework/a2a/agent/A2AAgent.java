package github.ponyhuang.agentframework.a2a.agent;

import github.ponyhuang.agentframework.a2a.client.A2AClient;
import github.ponyhuang.agentframework.a2a.client.A2AClientImpl;
import github.ponyhuang.agentframework.a2a.types.*;
import github.ponyhuang.agentframework.agents.BaseAgent;
import github.ponyhuang.agentframework.sessions.AgentSession;
import github.ponyhuang.agentframework.sessions.InMemoryAgentSession;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.Content;
import github.ponyhuang.agentframework.types.Message;
import github.ponyhuang.agentframework.types.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.regex.Pattern;

public class A2AAgent extends BaseAgent {
    private static final Logger LOG = LoggerFactory.getLogger(A2AAgent.class);
    private static final Pattern URI_PATTERN = Pattern.compile("data:(?<media_type>[^;]+);base64,(?<base64_data>[A-Za-z0-9+/=]+)");

    private static final Set<TaskState> TERMINAL_STATES = Set.of(
            TaskState.COMPLETED, TaskState.FAILED, TaskState.CANCELED, TaskState.REJECTED);
    private static final Set<TaskState> IN_PROGRESS_STATES = Set.of(
            TaskState.SUBMITTED, TaskState.WORKING, TaskState.INPUT_REQUIRED, TaskState.AUTH_REQUIRED);

    private final A2AClient client;
    private final boolean closeClient;
    private final String agentName;
    private final String description;

    private A2AAgent(Builder builder) {
        super(builder);
        this.client = builder.client;
        this.closeClient = builder.closeClient;
        this.agentName = builder.agentName;
        this.description = builder.description;
    }

    @Override
    public String getName() {
        return agentName != null ? agentName : "A2A Agent";
    }

    @Override
    protected AgentSession createSession(Map<String, Object> options) {
        return new InMemoryAgentSession(this);
    }

    @Override
    protected ChatResponse doRun(List<Message> messages, Map<String, Object> options) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages cannot be empty");
        }

        Message lastMessage = messages.get(messages.size() - 1);
        github.ponyhuang.agentframework.a2a.types.Message a2aMessage = prepareMessageForA2A(lastMessage);

        List<ChatResponse> responses = new ArrayList<>();
        client.sendMessage(a2aMessage).toStream().forEach(event -> {
            ChatResponse response = convertEventToResponse(event);
            if (response != null) {
                responses.add(response);
            }
        });

        if (responses.isEmpty()) {
            return ChatResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .choices(List.of(new ChatResponse.Choice(0, Message.assistant(""), "stop")))
                    .build();
        }

        return responses.get(responses.size() - 1);
    }

    @Override
    protected Flux<ChatResponse> doRunStream(List<Message> messages, Map<String, Object> options) {
        if (messages == null || messages.isEmpty()) {
            return Flux.error(new IllegalArgumentException("Messages cannot be empty"));
        }

        Message lastMessage = messages.get(messages.size() - 1);
        github.ponyhuang.agentframework.a2a.types.Message a2aMessage = prepareMessageForA2A(lastMessage);

        return client.sendMessage(a2aMessage)
                .map(this::convertEventToResponse)
                .filter(Objects::nonNull);
    }

    private ChatResponse convertEventToResponse(Object event) {
        if (event instanceof github.ponyhuang.agentframework.a2a.types.Message) {
            github.ponyhuang.agentframework.a2a.types.Message a2aMsg = (github.ponyhuang.agentframework.a2a.types.Message) event;
            List<Content> contents = parseContentsFromA2A(a2aMsg.getParts());
            Message frameworkMessage = Message.builder()
                    .role(a2aMsg.getRole() == github.ponyhuang.agentframework.a2a.types.Role.AGENT ? Role.ASSISTANT : Role.USER)
                    .contents(contents)
                    .build();

            return ChatResponse.builder()
                    .id(a2aMsg.getMessageId() != null ? a2aMsg.getMessageId() : UUID.randomUUID().toString())
                    .choices(List.of(new ChatResponse.Choice(0, frameworkMessage, "stop")))
                    .build();
        } else if (event instanceof Task) {
            Task task = (Task) event;
            List<Message> messages = parseMessagesFromTask(task);
            if (!messages.isEmpty()) {
                Message frameworkMessage = messages.get(messages.size() - 1);
                return ChatResponse.builder()
                        .id(task.getId())
                        .choices(List.of(new ChatResponse.Choice(0, frameworkMessage, task.getState().name().toLowerCase())))
                        .build();
            }
            return ChatResponse.builder()
                    .id(task.getId())
                    .choices(List.of(new ChatResponse.Choice(0, Message.assistant(""), task.getState().name().toLowerCase())))
                    .build();
        }
        return null;
    }

    private github.ponyhuang.agentframework.a2a.types.Message prepareMessageForA2A(Message message) {
        if (message.getContents() == null || message.getContents().isEmpty()) {
            throw new IllegalArgumentException("Message.contents is empty; cannot convert to A2A Message.");
        }

        List<Part> parts = new ArrayList<>();
        for (Content content : message.getContents()) {
            Part part = convertContentToPart(content);
            if (part != null) {
                parts.add(part);
            }
        }

        return github.ponyhuang.agentframework.a2a.types.Message.builder()
                .role(github.ponyhuang.agentframework.a2a.types.Role.USER)
                .parts(parts)
                .messageId(UUID.randomUUID().toString())
                .metadata(null)
                .build();
    }

    private Part convertContentToPart(Content content) {
        if (content == null) return null;

        switch (content.getType()) {
            case TEXT:
                return new TextPart(content.getText(), null);
            case IMAGE:
            case AUDIO:
            case VIDEO:
                return new FilePart(new FileWithUri(content.getText(), content.getType().getValue()),
                        null);
            default:
                LOG.warn("Unsupported content type: {}", content.getType());
                return new TextPart("Unsupported content type: " + content.getType());
        }
    }

    private List<Content> parseContentsFromA2A(List<Part> parts) {
        List<Content> contents = new ArrayList<>();
        if (parts == null) return contents;

        for (Part part : parts) {
            if (part instanceof TextPart) {
                TextPart textPart = (TextPart) part;
                contents.add(Content.text(textPart.getText()));
            } else if (part instanceof FilePart) {
                FilePart filePart = (FilePart) part;
                FileReference file = filePart.getFile();
                if (file instanceof FileWithUri) {
                    FileWithUri fileWithUri = (FileWithUri) file;
                    contents.add(Content.builder()
                            .type(Content.ContentType.IMAGE)
                            .text(fileWithUri.getUri())
                            .build());
                } else if (file instanceof FileWithBytes) {
                    FileWithBytes fileWithBytes = (FileWithBytes) file;
                    String base64 = Base64.getEncoder().encodeToString(fileWithBytes.getDecodedBytes());
                    contents.add(Content.builder()
                            .type(Content.ContentType.IMAGE)
                            .text("data:" + fileWithBytes.getMimeType() + ";base64," + base64)
                            .build());
                }
            } else if (part instanceof DataPart) {
                DataPart dataPart = (DataPart) part;
                contents.add(Content.text(dataPart.getData().toString()));
            }
        }
        return contents;
    }

    private List<Message> parseMessagesFromTask(Task task) {
        List<Message> messages = new ArrayList<>();

        if (task.getArtifacts() != null) {
            for (Artifact artifact : task.getArtifacts()) {
                List<Content> contents = parseContentsFromA2A(artifact.getParts());
                messages.add(Message.builder()
                        .role(Role.ASSISTANT)
                        .contents(contents)
                        .build());
            }
        } else if (task.getHistory() != null && !task.getHistory().isEmpty()) {
            github.ponyhuang.agentframework.a2a.types.Message historyItem = task.getHistory().get(task.getHistory().size() - 1);
            List<Content> contents = parseContentsFromA2A(historyItem.getParts());
            messages.add(Message.builder()
                    .role(historyItem.getRole() == github.ponyhuang.agentframework.a2a.types.Role.AGENT ? Role.ASSISTANT : Role.USER)
                    .contents(contents)
                    .build());
        }

        return messages;
    }

    public A2AContinuationToken buildContinuationToken(Task task) {
        if (IN_PROGRESS_STATES.contains(task.getState())) {
            return new A2AContinuationToken(task.getId(), task.getContextId());
        }
        return null;
    }

    public ChatResponse pollTask(A2AContinuationToken token) {
        Task task = client.getTask(token.getTaskId());
        List<Message> messages = parseMessagesFromTask(task);

        if (!messages.isEmpty()) {
            return ChatResponse.builder()
                    .id(task.getId())
                    .choices(List.of(new ChatResponse.Choice(0, messages.get(messages.size() - 1),
                            task.getState().name().toLowerCase())))
                    .build();
        }

        return ChatResponse.builder()
                .id(task.getId())
                .choices(List.of(new ChatResponse.Choice(0, Message.assistant(""), task.getState().name().toLowerCase())))
                .build();
    }

    public AgentCard getAgentCard() {
        return client.getAgentCard();
    }

    public void close() {
        if (closeClient && client != null) {
            client.close();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends BaseAgent.Builder<A2AAgent, Builder> {
        private A2AClient client;
        private boolean closeClient = false;
        private String agentName;
        private String description;

        public Builder() {
            this.agentName = "assistant";
        }

        public Builder name(String name) {
            this.agentName = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder client(A2AClient client) {
            this.client = client;
            return this;
        }

        public Builder url(String url) {
            this.client = A2AClientImpl.builder().url(url).build();
            this.closeClient = true;
            return this;
        }

        public Builder agentCard(AgentCard agentCard) {
            this.client = A2AClientImpl.builder().agentCard(agentCard).build();
            this.closeClient = true;
            return this;
        }

        @Override
        public A2AAgent build() {
            if (client == null) {
                throw new IllegalArgumentException("A2AClient, url, or agentCard must be provided");
            }
            return new A2AAgent(this);
        }
    }
}

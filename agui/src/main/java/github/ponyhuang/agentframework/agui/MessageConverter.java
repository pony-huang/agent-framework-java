package github.ponyhuang.agentframework.agui;

import com.agui.core.message.*;
import github.ponyhuang.agentframework.types.Content;
import github.ponyhuang.agentframework.types.Message;
import github.ponyhuang.agentframework.types.Role;

import java.util.List;
import java.util.stream.Collectors;

public class MessageConverter {

    public static List<Message> toFrameworkMessages(List<BaseMessage> aguiMessages) {
        if (aguiMessages == null) {
            return List.of();
        }
        return aguiMessages.stream()
                .map(MessageConverter::toFrameworkMessage)
                .collect(Collectors.toList());
    }

    public static Message toFrameworkMessage(BaseMessage aguiMessage) {
        if (aguiMessage == null) {
            return null;
        }

        Role role;
        String content = aguiMessage.getContent();
        if (content == null) {
            content = "";
        }

        if (aguiMessage instanceof UserMessage) {
            role = Role.USER;
            return Message.user(content);
        } else if (aguiMessage instanceof AssistantMessage) {
            role = Role.ASSISTANT;
            return Message.assistant(content);
        } else if (aguiMessage instanceof ToolMessage toolMessage) {
            String toolCallId = toolMessage.getId();
            String toolName = toolMessage.getName();
            if (toolName != null) {
                return Message.builder()
                        .role(Role.TOOL)
                        .toolCallId(toolCallId)
                        .addContent(Content.text(toolMessage.getContent() != null ? toolMessage.getContent() : ""))
                        .build();
            }
            return Message.builder()
                    .role(Role.TOOL)
                    .toolCallId(toolCallId)
                    .addContent(Content.text(""))
                    .build();
        } else if (aguiMessage instanceof DeveloperMessage) {
            role = Role.SYSTEM;
            return Message.system(content);
        } else if (aguiMessage instanceof SystemMessage) {
            role = Role.SYSTEM;
            return Message.system(content);
        } else {
            role = Role.USER;
            return Message.user(content);
        }
    }

    public static List<BaseMessage> toAguiMessages(List<Message> frameworkMessages) {
        if (frameworkMessages == null) {
            return List.of();
        }
        return frameworkMessages.stream()
                .map(MessageConverter::toAguiMessage)
                .collect(Collectors.toList());
    }

    public static BaseMessage toAguiMessage(Message message) {
        if (message == null) {
            return null;
        }

        String content = message.getText();
        if (content == null) {
            content = "";
        }

        return switch (message.getRole()) {
            case USER -> {
                UserMessage msg = new UserMessage();
                msg.setContent(content);
                yield msg;
            }
            case ASSISTANT -> {
                AssistantMessage msg = new AssistantMessage();
                msg.setContent(content);
                yield msg;
            }
            case SYSTEM -> {
                SystemMessage msg = new SystemMessage();
                msg.setContent(content);
                yield msg;
            }
            case TOOL -> {
                ToolMessage msg = new ToolMessage();
                msg.setId(message.getToolCallId());
                msg.setName(message.getFunctionCall() != null
                        ? (String) message.getFunctionCall().get("name")
                        : "unknown");
                msg.setContent(content);
                yield msg;
            }
            default -> {
                UserMessage msg = new UserMessage();
                msg.setContent(content);
                yield msg;
            }
        };
    }
}

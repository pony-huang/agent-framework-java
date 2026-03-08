//package github.ponyhuang.agentframework.agui;
//
//import com.agui.core.message.*;
//import github.ponyhuang.agentframework.types.block.ToolUseBlock;
//import github.ponyhuang.agentframework.types.message.Message;
//
//import java.util.List;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//public class MessageConverter {
//
//    public static List<Message> toFrameworkMessages(List<BaseMessage> aguiMessages) {
//        if (aguiMessages == null) {
//            return List.of();
//        }
//        return aguiMessages.stream()
//                .map(MessageConverter::toFrameworkMessage)
//                .collect(Collectors.toList());
//    }
//
//    public static Message toFrameworkMessage(BaseMessage aguiMessage) {
//        if (aguiMessage == null) {
//            return null;
//        }
//
//        String content = aguiMessage.getContent();
//        if (content == null) {
//            content = "";
//        }
//
//        if (aguiMessage instanceof UserMessage) {
//            return github.ponyhuang.agentframework.types.message.UserMessage.create(content);
//        } else if (aguiMessage instanceof AssistantMessage) {
//            return github.ponyhuang.agentframework.types.message.AssistantMessage.create(content);
//        } else if (aguiMessage instanceof ToolMessage toolMessage) {
//            String toolCallId = toolMessage.getId();
//            String toolName = toolMessage.getName();
//            return github.ponyhuang.agentframework.types.message.AssistantMessage.create(List.of(ToolUseBlock.of(toolCallId, Objects.requireNonNullElse(toolName, ""), null)));
//        } else if (aguiMessage instanceof DeveloperMessage) {
//            return github.ponyhuang.agentframework.types.message.AssistantMessage.create(content);
//        } else if (aguiMessage instanceof SystemMessage) {
//            return github.ponyhuang.agentframework.types.message.SystemMessage.create(content);
//        } else {
//            return github.ponyhuang.agentframework.types.message.SystemMessage.create(content);
//        }
//    }
//
//    public static List<BaseMessage> toAguiMessages(List<Message> frameworkMessages) {
//        if (frameworkMessages == null) {
//            return List.of();
//        }
//        return frameworkMessages.stream()
//                .map(MessageConverter::toAguiMessage)
//                .collect(Collectors.toList());
//    }
//
//    public static BaseMessage toAguiMessage(Message message) {
//        if (message == null) {
//            return null;
//        }
//
//        String content = message.getTextContent();
//        if (content == null) {
//            content = "";
//        }
//
//        if ("user".equals(message.getRole())) {
//            UserMessage msg = new UserMessage();
//            msg.setContent(content);
//            return msg;
//        } else if ("assistant".equals(message.getRole())) {
//            AssistantMessage msg = new AssistantMessage();
//            msg.setContent(content);
//            return msg;
//        } else if ("system".equals(message.getRole())) {
//            SystemMessage msg = new SystemMessage();
//            msg.setContent(content);
//            return msg;
//        } else {
//            UserMessage msg = new UserMessage();
//            msg.setContent(content);
//            return msg;
//        }
//    }
//}

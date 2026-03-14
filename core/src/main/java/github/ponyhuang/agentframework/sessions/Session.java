package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.message.Message;

import java.util.List;
import java.util.Map;

public interface Session {

    String getId();

    List<Message> getMessages();

    void addMessage(Message message);

    void addMessages(List<Message> messages);

    List<Message> getHistory(int limit);

    void clearHistory();

    Map<String, Object> getMetadata();

    void setMetadata(String key, Object value);

    Object getMetadata(String key);
}

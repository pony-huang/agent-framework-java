package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.message.Message;

public interface AgentSession extends ConversationSession, SessionExecutor {

    Agent getAgent();

    default boolean isClosed() {
        return false;
    }
}

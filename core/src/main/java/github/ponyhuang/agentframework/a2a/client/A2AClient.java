package github.ponyhuang.agentframework.a2a.client;

import github.ponyhuang.agentframework.a2a.types.AgentCard;
import github.ponyhuang.agentframework.a2a.types.Message;
import github.ponyhuang.agentframework.a2a.types.Task;
import reactor.core.publisher.Flux;

/**
 * Interface for A2A client operations.
 */
public interface A2AClient {

    /**
     * Get the agent card for this client.
     *
     * @return the agent card
     */
    AgentCard getAgentCard();

    /**
     * Send a message to the A2A agent.
     *
     * @param message the message to send
     * @return a flux of A2A events
     */
    Flux<A2AEvent> sendMessage(Message message);

    /**
     * Get a task by ID.
     *
     * @param taskId the task ID
     * @return the task
     */
    Task getTask(String taskId);

    /**
     * Resubscribe to a task to receive live updates.
     *
     * @param taskId the task ID to resubscribe to
     * @return a flux of A2A events
     */
    Flux<A2AEvent> resubscribe(String taskId);

    /**
     * Close the client and release resources.
     */
    void close();
}

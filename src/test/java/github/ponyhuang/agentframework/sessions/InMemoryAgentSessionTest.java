package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryAgentSession class.
 * Tests message storage and retrieval.
 */
class InMemoryAgentSessionTest {

    private InMemoryAgentSession session;
    private Agent mockAgent;

    @BeforeEach
    void setUp() {
        // Create mock agent
        mockAgent = Mockito.mock(Agent.class);
        Mockito.when(mockAgent.getName()).thenReturn("testAgent");

        // Create session with mock agent
        session = new InMemoryAgentSession(mockAgent);
    }

    /**
     * Test InMemoryAgentSession constructor initializes correctly.
     * Verifies session is created with empty message list.
     */
    @Test
    void testConstructorInitializesCorrectly() {
        // Verify initial state
        assertNotNull(session.getId());
        assertEquals(0, session.getMessages().size());
    }

    /**
     * Test getAgent() returns the agent.
     * Verifies agent getter.
     */
    @Test
    void testGetAgent() {
        // Verify agent is returned
        assertEquals(mockAgent, session.getAgent());
    }

    /**
     * Test getId() returns unique ID.
     * Verifies ID generation.
     */
    @Test
    void testGetIdReturnsUniqueId() {
        // Get ID
        String id = session.getId();

        // Verify ID is not null and not empty
        assertNotNull(id);
        assertFalse(id.isEmpty());
    }

    /**
     * Test addMessage() adds message to session.
     * Verifies message storage.
     */
    @Test
    void testAddMessage() {
        // Add a message
        Message message = Message.user("Hello");
        session.addMessage(message);

        // Verify message was added
        assertEquals(1, session.getMessages().size());
        assertEquals("Hello", session.getMessages().get(0).getText());
    }

    /**
     * Test addMessage() with null message does nothing.
     * Verifies null handling.
     */
    @Test
    void testAddMessageWithNull() {
        // Add null message
        session.addMessage(null);

        // Verify no message was added
        assertEquals(0, session.getMessages().size());
    }

    /**
     * Test addMessages() adds multiple messages.
     * Verifies batch message addition.
     */
    @Test
    void testAddMessages() {
        // Add multiple messages
        List<Message> messages = List.of(
            Message.user("Hello"),
            Message.assistant("Hi there!")
        );
        session.addMessages(messages);

        // Verify all messages were added
        assertEquals(2, session.getMessages().size());
    }

    /**
     * Test addMessages() with null list does nothing.
     * Verifies null handling.
     */
    @Test
    void testAddMessagesWithNull() {
        // Add null list
        session.addMessages(null);

        // Verify no messages were added
        assertEquals(0, session.getMessages().size());
    }

    /**
     * Test getHistory() returns all messages when limit is 0.
     * Verifies getHistory with no limit.
     */
    @Test
    void testGetHistoryNoLimit() {
        // Add messages
        session.addMessage(Message.user("First"));
        session.addMessage(Message.user("Second"));
        session.addMessage(Message.user("Third"));

        // Get history with limit 0
        List<Message> history = session.getHistory(0);

        // Verify all messages returned
        assertEquals(3, history.size());
    }

    /**
     * Test getHistory() returns limited messages.
     * Verifies getHistory with limit.
     */
    @Test
    void testGetHistoryWithLimit() {
        // Add messages
        session.addMessage(Message.user("First"));
        session.addMessage(Message.user("Second"));
        session.addMessage(Message.user("Third"));

        // Get history with limit
        List<Message> history = session.getHistory(2);

        // Verify only last 2 messages returned
        assertEquals(2, history.size());
        assertEquals("Second", history.get(0).getText());
        assertEquals("Third", history.get(1).getText());
    }

    /**
     * Test getHistory() returns all when limit exceeds size.
     * Verifies limit greater than size.
     */
    @Test
    void testGetHistoryLimitExceedsSize() {
        // Add only 2 messages
        session.addMessage(Message.user("First"));
        session.addMessage(Message.user("Second"));

        // Get history with large limit
        List<Message> history = session.getHistory(100);

        // Verify all messages returned
        assertEquals(2, history.size());
    }

    /**
     * Test clearHistory() removes all messages.
     * Verifies clearHistory method.
     */
    @Test
    void testClearHistory() {
        // Add messages
        session.addMessage(Message.user("Hello"));
        session.addMessage(Message.user("World"));

        // Clear history
        session.clearHistory();

        // Verify all messages removed
        assertEquals(0, session.getMessages().size());
    }

    /**
     * Test getMetadata() returns empty map initially.
     * Verifies metadata initialization.
     */
    @Test
    void testGetMetadata() {
        // Get metadata
        var metadata = session.getMetadata();

        // Verify empty map returned
        assertNotNull(metadata);
        assertTrue(metadata.isEmpty());
    }

    /**
     * Test setMetadata() and getMetadata() store and retrieve values.
     * Verifies metadata operations.
     */
    @Test
    void testMetadataOperations() {
        // Set metadata
        session.setMetadata("key1", "value1");
        session.setMetadata("key2", 42);

        // Verify metadata
        assertEquals("value1", session.getMetadata("key1"));
        assertEquals(42, session.getMetadata("key2"));
    }

    /**
     * Test getMessages() returns copy of messages.
     * Verifies returned list is a copy.
     */
    @Test
    void testGetMessagesReturnsCopy() {
        // Add message
        session.addMessage(Message.user("Hello"));

        // Get messages twice
        List<Message> messages1 = session.getMessages();
        List<Message> messages2 = session.getMessages();

        // Verify they are different instances (copies)
        assertNotSame(messages1, messages2);
    }
}

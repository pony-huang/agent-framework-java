package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionManager.
 */
class SessionManagerTest {

    private SessionManager sessionManager;
    private InMemorySessionStore sessionStore;

    @BeforeEach
    void setUp() {
        sessionStore = new InMemorySessionStore();
        sessionManager = new SessionManager();
        sessionManager.setStore(sessionStore);
    }

    @Test
    void testCreateSession() {
        Session session = sessionManager.createSession();

        assertNotNull(session.getId());
        assertTrue(session.isActive());
    }

    @Test
    void testGetSession() {
        Session created = sessionManager.createSession();
        Optional<Session> retrieved = sessionManager.getSession(created.getId());

        assertTrue(retrieved.isPresent());
        assertEquals(created.getId(), retrieved.get().getId());
    }

    @Test
    void testGetNonExistentSession() {
        Optional<Session> retrieved = sessionManager.getSession("non-existent");

        assertFalse(retrieved.isPresent());
    }

    @Test
    void testResumeSession() {
        Session original = sessionManager.createSession();
        original.addMessage(UserMessage.create("Test message"));
        String sessionId = original.getId();

        // Simulate session being stored
        sessionStore.save(original);

        // Resume the session
        Optional<Session> resumed = sessionManager.resumeSession(sessionId);

        assertTrue(resumed.isPresent());
        assertEquals(1, resumed.get().getMessages().size());
    }

    @Test
    void testResumeSessionNotFound() {
        Optional<Session> resumed = sessionManager.resumeSession("non-existent");

        assertFalse(resumed.isPresent());
    }

    @Test
    void testDeleteSession() {
        Session session = sessionManager.createSession();
        String sessionId = session.getId();

        sessionManager.deleteSession(sessionId);

        Optional<Session> retrieved = sessionManager.getSession(sessionId);
        assertFalse(retrieved.isPresent());
    }

    @Test
    void testListSessions() {
        sessionManager.createSession();
        sessionManager.createSession();
        sessionManager.createSession();

        var sessions = sessionManager.listSessions();

        assertEquals(3, sessions.size());
    }
}

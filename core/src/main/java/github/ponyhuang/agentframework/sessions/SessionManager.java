package github.ponyhuang.agentframework.sessions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manager for Session lifecycle.
 * Handles creation, retrieval, deletion, and timeout monitoring of sessions.
 */
public class SessionManager {

    private static final Logger LOG = LoggerFactory.getLogger(SessionManager.class);

    private SessionStore store;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private long defaultTimeoutMs = 30 * 60 * 1000; // 30 minutes
    private int defaultMaxMessages = 100;
    private long checkIntervalMs = 60 * 1000; // 1 minute

    private volatile boolean running = false;

    public SessionManager() {
    }

    /**
     * Creates a new session with default settings.
     *
     * @return the created session
     */
    public Session createSession() {
        DefaultSession session = new DefaultSession(UUID.randomUUID().toString());
        session.setTimeoutMs(defaultTimeoutMs);
        session.setMaxMessages(defaultMaxMessages);
        session.start();

        if (store != null) {
            store.save(session);
        }

        LOG.info("Created new session: {}", session.getId());
        return session;
    }

    /**
     * Gets a session by ID.
     *
     * @param id the session ID
     * @return the session if found
     */
    public Optional<Session> getSession(String id) {
        if (store != null) {
            return store.findById(id);
        }
        return Optional.empty();
    }

    /**
     * Resume a session from storage.
     *
     * @param id the session ID to resume
     * @return the resumed session, or empty if not found
     */
    public Optional<Session> resumeSession(String id) {
        Optional<Session> session = getSession(id);
        if (session.isPresent()) {
            session.get().updateLastActiveTime();
            LOG.info("Resumed session: {}", id);
        } else {
            LOG.warn("Session not found for resume: {}", id);
        }
        return session;
    }

    /**
     * Deletes a session.
     *
     * @param id the session ID
     */
    public void deleteSession(String id) {
        if (store != null) {
            store.delete(id);
            LOG.info("Deleted session: {}", id);
        }
    }

    /**
     * Lists all sessions.
     *
     * @return list of all sessions
     */
    public List<Session> listSessions() {
        if (store != null) {
            return store.findAll();
        }
        return List.of();
    }

    /**
     * Sets the session store.
     *
     * @param store the session store
     */
    public void setStore(SessionStore store) {
        this.store = store;
    }

    /**
     * Sets the default timeout for new sessions.
     *
     * @param timeoutMs timeout in milliseconds
     */
    public void setDefaultTimeoutMs(long timeoutMs) {
        this.defaultTimeoutMs = timeoutMs;
    }

    /**
     * Sets the default max messages for new sessions.
     *
     * @param maxMessages maximum number of messages
     */
    public void setDefaultMaxMessages(int maxMessages) {
        this.defaultMaxMessages = maxMessages;
    }

    /**
     * Starts the timeout monitoring.
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        scheduler.scheduleAtFixedRate(
            this::checkTimeouts,
            checkIntervalMs,
            checkIntervalMs,
            TimeUnit.MILLISECONDS
        );

        LOG.info("SessionManager started with {}ms check interval", checkIntervalMs);
    }

    /**
     * Stops the timeout monitoring.
     */
    public void stop() {
        running = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        LOG.info("SessionManager stopped");
    }

    private void checkTimeouts() {
        if (store == null) {
            return;
        }

        List<Session> sessions = store.findAll();
        for (Session session : sessions) {
            if (session instanceof DefaultSession defaultSession) {
                if (defaultSession.isExpired()) {
                    defaultSession.end();
                    LOG.info("Session expired: {}", session.getId());
                }
            }
        }
    }

    public SessionStore getStore() {
        return store;
    }

    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public int getDefaultMaxMessages() {
        return defaultMaxMessages;
    }
}

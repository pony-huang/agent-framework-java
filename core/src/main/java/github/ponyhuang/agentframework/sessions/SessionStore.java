package github.ponyhuang.agentframework.sessions;

import java.util.List;
import java.util.Optional;

/**
 * Interface for session storage implementations.
 * Supports various backends like in-memory, Redis, MongoDB, etc.
 */
public interface SessionStore {

    /**
     * Saves a session to the store.
     *
     * @param session the session to save
     */
    void save(Session session);

    /**
     * Finds a session by its ID.
     *
     * @param id the session ID
     * @return the session if found, empty otherwise
     */
    Optional<Session> findById(String id);

    /**
     * Deletes a session by its ID.
     *
     * @param id the session ID
     */
    void delete(String id);

    /**
     * Returns all sessions in the store.
     *
     * @return list of all sessions
     */
    List<Session> findAll();
}

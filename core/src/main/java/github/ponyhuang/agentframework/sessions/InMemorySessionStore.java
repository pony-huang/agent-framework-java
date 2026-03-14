package github.ponyhuang.agentframework.sessions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of SessionStore.
 * Thread-safe using ConcurrentHashMap.
 */
public class InMemorySessionStore implements SessionStore {

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(Session session) {
        if (session != null && session.getId() != null) {
            sessions.put(session.getId(), session);
        }
    }

    @Override
    public Optional<Session> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public void delete(String id) {
        if (id != null) {
            sessions.remove(id);
        }
    }

    @Override
    public List<Session> findAll() {
        return new ArrayList<>(sessions.values());
    }
}

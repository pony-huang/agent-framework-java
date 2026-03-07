package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for SessionStart hook event.
 * Fired when a session begins or resumes.
 */
public class SessionStartContext extends HookContext {

    private String source; // startup, resume, clear, compact
    private String model;
    // agentType is inherited from HookContext

    public SessionStartContext() {
        super(HookEvent.SESSION_START);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("source", source);
        map.put("model", model);
        return map;
    }
}

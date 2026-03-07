package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for WorktreeCreate hook event.
 * Fired when a worktree is being created.
 */
public class WorktreeCreateContext extends HookContext {

    private String name;

    public WorktreeCreateContext() {
        super(HookEvent.WORKTREE_CREATE);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("name", name);
        return map;
    }
}

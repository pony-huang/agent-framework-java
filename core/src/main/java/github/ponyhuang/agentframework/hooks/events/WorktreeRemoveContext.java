package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for WorktreeRemove hook event.
 * Fired when a worktree is being removed.
 */
public class WorktreeRemoveContext extends HookContext {

    private String worktreePath;

    public WorktreeRemoveContext() {
        super(HookEvent.WORKTREE_REMOVE);
    }

    public String getWorktreePath() {
        return worktreePath;
    }

    public void setWorktreePath(String worktreePath) {
        this.worktreePath = worktreePath;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("worktree_path", worktreePath);
        return map;
    }
}

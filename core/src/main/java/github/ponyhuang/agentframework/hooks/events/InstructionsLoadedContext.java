package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.List;
import java.util.Map;

/**
 * Context for InstructionsLoaded hook event.
 * Fired when CLAUDE.md or rules file is loaded.
 */
public class InstructionsLoadedContext extends HookContext {

    private String filePath;
    private String memoryType; // User, Project, Local, Managed
    private String loadReason; // session_start, nested_traversal, path_glob_match, include
    private List<String> globs;
    private String triggerFilePath;
    private String parentFilePath;

    public InstructionsLoadedContext() {
        super(HookEvent.INSTRUCTIONS_LOADED);
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMemoryType() {
        return memoryType;
    }

    public void setMemoryType(String memoryType) {
        this.memoryType = memoryType;
    }

    public String getLoadReason() {
        return loadReason;
    }

    public void setLoadReason(String loadReason) {
        this.loadReason = loadReason;
    }

    public List<String> getGlobs() {
        return globs;
    }

    public void setGlobs(List<String> globs) {
        this.globs = globs;
    }

    public String getTriggerFilePath() {
        return triggerFilePath;
    }

    public void setTriggerFilePath(String triggerFilePath) {
        this.triggerFilePath = triggerFilePath;
    }

    public String getParentFilePath() {
        return parentFilePath;
    }

    public void setParentFilePath(String parentFilePath) {
        this.parentFilePath = parentFilePath;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("file_path", filePath);
        map.put("memory_type", memoryType);
        map.put("load_reason", loadReason);
        map.put("globs", globs);
        map.put("trigger_file_path", triggerFilePath);
        map.put("parent_file_path", parentFilePath);
        return map;
    }
}

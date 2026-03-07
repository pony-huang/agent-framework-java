package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for ConfigChange hook event.
 * Fired when a configuration file changes during a session.
 */
public class ConfigChangeContext extends HookContext {

    private String source; // user_settings, project_settings, local_settings, policy_settings, skills
    private String filePath;

    public ConfigChangeContext() {
        super(HookEvent.CONFIG_CHANGE);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("source", source);
        map.put("file_path", filePath);
        return map;
    }
}

package github.ponyhuang.agentframework.hooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root configuration for hooks.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HookConfig {

    @JsonProperty("hooks")
    private Map<String, List<HookMatcherGroup>> hooks;

    @JsonProperty("disableAllHooks")
    private boolean disableAllHooks = false;

    public Map<String, List<HookMatcherGroup>> getHooks() {
        return hooks;
    }

    public void setHooks(Map<String, List<HookMatcherGroup>> hooks) {
        this.hooks = hooks;
    }

    public boolean isDisableAllHooks() {
        return disableAllHooks;
    }

    public void setDisableAllHooks(boolean disableAllHooks) {
        this.disableAllHooks = disableAllHooks;
    }

    /**
     * Converts string-based hooks map to HookEvent-based map.
     */
    public Map<HookEvent, List<HookExecutor.HookRegistration>> toRegistrationMap() {
        Map<HookEvent, List<HookExecutor.HookRegistration>> result = new HashMap<>();

        if (hooks == null) {
            return result;
        }

        for (Map.Entry<String, List<HookMatcherGroup>> entry : hooks.entrySet()) {
            HookEvent event = HookEvent.fromEventName(entry.getKey());
            if (event == null) {
                continue;
            }

            List<HookExecutor.HookRegistration> registrations = new java.util.ArrayList<>();
            for (HookMatcherGroup group : entry.getValue()) {
                HookMatcher matcher = group.getMatcher() != null ?
                        new HookMatcher(group.getMatcher()) : null;

                for (HookHandlerConfig handlerConfig : group.getHooks()) {
                    HookHandler handler = handlerConfig.toHandler();
                    if (handler != null) {
                        registrations.add(new HookExecutor.HookRegistration(matcher, handler));
                    }
                }
            }

            result.put(event, registrations);
        }

        return result;
    }
}

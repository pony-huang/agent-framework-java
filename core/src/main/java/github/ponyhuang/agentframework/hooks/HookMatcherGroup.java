package github.ponyhuang.agentframework.hooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Group of hooks with a matcher.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HookMatcherGroup {

    @JsonProperty("matcher")
    private String matcher;

    @JsonProperty("hooks")
    private List<HookHandlerConfig> hooks;

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public List<HookHandlerConfig> getHooks() {
        return hooks;
    }

    public void setHooks(List<HookHandlerConfig> hooks) {
        this.hooks = hooks;
    }
}

package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for TeammateIdle hook event.
 * Fired when an agent teammate is about to go idle.
 */
public class TeammateIdleContext extends HookContext {

    private String teammateName;
    private String teamName;

    public TeammateIdleContext() {
        super(HookEvent.TEAMMATE_IDLE);
    }

    public String getTeammateName() {
        return teammateName;
    }

    public void setTeammateName(String teammateName) {
        this.teammateName = teammateName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("teammate_name", teammateName);
        map.put("team_name", teamName);
        return map;
    }
}

package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for UserPromptSubmit hook event.
 * Fired when user submits a prompt before processing.
 */
public class UserPromptSubmitContext extends HookContext {

    private String prompt;

    public UserPromptSubmitContext() {
        super(HookEvent.USER_PROMPT_SUBMIT);
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("prompt", prompt);
        return map;
    }
}

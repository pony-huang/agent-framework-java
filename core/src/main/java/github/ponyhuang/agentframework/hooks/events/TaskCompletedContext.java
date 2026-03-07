package github.ponyhuang.agentframework.hooks.events;

import github.ponyhuang.agentframework.hooks.HookContext;
import github.ponyhuang.agentframework.hooks.HookEvent;

import java.util.Map;

/**
 * Context for TaskCompleted hook event.
 * Fired when a task is being marked as completed.
 */
public class TaskCompletedContext extends HookContext {

    private String taskId;
    private String taskSubject;
    private String taskDescription;
    private String teammateName;
    private String teamName;

    public TaskCompletedContext() {
        super(HookEvent.TASK_COMPLETED);
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskSubject() {
        return taskSubject;
    }

    public void setTaskSubject(String taskSubject) {
        this.taskSubject = taskSubject;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
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
        map.put("task_id", taskId);
        map.put("task_subject", taskSubject);
        map.put("task_description", taskDescription);
        map.put("teammate_name", teammateName);
        map.put("team_name", teamName);
        return map;
    }
}

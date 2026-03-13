package github.ponyhuang.agentframework.tools.builtins;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Built-in tools for task management.
 * Provides create, update, list, and get operations.
 */
public class TaskTools {

    private static final Logger LOG = LoggerFactory.getLogger(TaskTools.class);

    // In-memory task storage
    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public TaskTools() {
    }

    /**
     * Create a new task.
     */
    @Tool(description = "Create a new task with a subject and description.")
    public String create(
            @ToolParam(description = "The task subject/title") String subject,
            @ToolParam(description = "Detailed description of the task") String description) {
        return createWithOptions(subject, description, null);
    }

    /**
     * Create a new task with all options.
     */
    @Tool(description = "Create a new task with subject, description, and optional activeForm.")
    public String createWithOptions(
            @ToolParam(description = "The task subject/title") String subject,
            @ToolParam(description = "Detailed description of the task") String description,
            @ToolParam(description = "Active form description (present continuous tense)", required = false) String activeForm) {

        String id = String.valueOf(idCounter.getAndIncrement());
        Task task = new Task(id, subject, description, activeForm);
        tasks.put(id, task);

        LOG.info("Created task: {} - {}", id, subject);
        return "Created task " + id + ": " + subject;
    }

    /**
     * Update an existing task.
     */
    @Tool(description = "Update an existing task. Can update status, subject, description, or other fields.")
    public String update(
            @ToolParam(description = "The task ID to update") String taskId,
            @ToolParam(description = "New status (pending, in_progress, completed)", required = false) String status,
            @ToolParam(description = "New subject", required = false) String subject,
            @ToolParam(description = "New description", required = false) String description,
            @ToolParam(description = "New activeForm", required = false) String activeForm) {

        Task task = tasks.get(taskId);
        if (task == null) {
            return "Error: Task not found: " + taskId;
        }

        if (status != null) {
            task.status = status;
        }
        if (subject != null) {
            task.subject = subject;
        }
        if (description != null) {
            task.description = description;
        }
        if (activeForm != null) {
            task.activeForm = activeForm;
        }

        LOG.info("Updated task: {}", taskId);
        return "Updated task " + taskId;
    }

    /**
     * List all tasks.
     */
    @Tool(description = "List all tasks with their details.")
    public List<Task> list() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Get a specific task by ID.
     */
    @Tool(description = "Get a specific task by its ID.")
    public Task get(
            @ToolParam(description = "The task ID to retrieve") String taskId) {
        return tasks.get(taskId);
    }

    /**
     * Delete a task.
     */
    @Tool(description = "Delete a task by its ID.")
    public String delete(
            @ToolParam(description = "The task ID to delete") String taskId) {
        Task removed = tasks.remove(taskId);
        if (removed == null) {
            return "Error: Task not found: " + taskId;
        }
        LOG.info("Deleted task: {}", taskId);
        return "Deleted task " + taskId;
    }

    /**
     * Internal Task class for storage.
     */
    public static class Task {
        public String id;
        public String subject;
        public String description;
        public String status;
        public String activeForm;
        public Date createdAt;
        public Date updatedAt;

        public Task(String id, String subject, String description, String activeForm) {
            this.id = id;
            this.subject = subject;
            this.description = description;
            this.activeForm = activeForm;
            this.status = "pending";
            this.createdAt = new Date();
            this.updatedAt = new Date();
        }
    }
}

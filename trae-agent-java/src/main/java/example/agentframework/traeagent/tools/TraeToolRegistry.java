package example.agentframework.traeagent.tools;

import github.ponyhuang.agentframework.tools.ToolExecutor;
import example.agentframework.traeagent.config.TraeAgentConfig;

import java.util.List;

/**
 * Registry for TraeAgent tools.
 * Registers and manages all available tools.
 */
public class TraeToolRegistry {

    private final ToolExecutor toolExecutor;
    private final TraeAgentConfig config;

    public TraeToolRegistry(TraeAgentConfig config) {
        this.config = config;
        this.toolExecutor = new ToolExecutor();
        registerTools();
    }

    private void registerTools() {
        // Get enabled tools from config
        List<String> enabledTools = config.getTools();

        // Register BashTool
        if (enabledTools.contains("bash")) {
            BashTool bashTool = new BashTool(config.getWorkingDirectory());
            toolExecutor.registerAnnotated(bashTool);
        }

        // Register EditTool
        if (enabledTools.contains("edit")) {
            EditTool editTool = new EditTool(config.getWorkingDirectory());
            toolExecutor.registerAnnotated(editTool);
        }

        // Register JSONEditTool
        if (enabledTools.contains("json_edit")) {
            JSONEditTool jsonEditTool = new JSONEditTool(config.getWorkingDirectory());
            toolExecutor.registerAnnotated(jsonEditTool);
        }

        // Register SequentialThinkingTool
        if (enabledTools.contains("sequential_thinking")) {
            SequentialThinkingTool thinkingTool = new SequentialThinkingTool();
            toolExecutor.registerAnnotated(thinkingTool);
        }

        // Register TaskDoneTool
        if (enabledTools.contains("task_done")) {
            TaskDoneTool taskDoneTool = new TaskDoneTool();
            toolExecutor.registerAnnotated(taskDoneTool);
        }
    }

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

    /**
     * Get the list of tool schemas for LLM.
     */
    public List<java.util.Map<String, Object>> getToolSchemas() {
        return toolExecutor.getToolSchemas();
    }
}
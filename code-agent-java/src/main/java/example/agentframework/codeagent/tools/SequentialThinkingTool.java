package example.agentframework.codeagent.tools;

import github.ponyhuang.agentframework.tools.Tool;
import github.ponyhuang.agentframework.tools.ToolParam;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Tool for sequential thinking and reasoning.
 * Helps the agent break down complex problems into steps.
 */
public class SequentialThinkingTool {

    private static final Logger LOG = LoggerFactory.getLogger(SequentialThinkingTool.class);

    // Store thinking history (thread-local for thread safety)
    private final ThreadLocal<Deque<ThinkingStep>> thinkingHistory = ThreadLocal.withInitial(LinkedList::new);

    @Tool(description = "Think through a problem step by step. " +
            "Use this when you need to reason about complex problems, plan multiple steps, or analyze options. " +
            "Can be used multiple times to build up a thinking process.")
    public String think(
            @ToolParam(description = "Your current thinking or thought") String thought,
            @ToolParam(description = "The next step to take (think, validate, or conclude)", required = false) String nextStep,
            @ToolParam(description = "What you want to achieve with this thought", required = false) String goal) {

        Deque<ThinkingStep> history = thinkingHistory.get();

        // Determine the thought type
        ThinkingStep step = getThinkingStep(thought, nextStep, goal);
        history.addLast(step);

        // Build response
        StringBuilder response = new StringBuilder();
        response.append("## Thinking\n\n");

        // Show recent thoughts (last 5)
        int count = 0;
        Iterator<ThinkingStep> iterator = history.descendingIterator();
        while (iterator.hasNext() && count < 5) {
            ThinkingStep s = iterator.next();
            response.append(String.format("%d. [%s] %s\n",
                    history.size() - count,
                    s.type.name(),
                    s.thought));
            if (s.goal != null && !s.goal.isEmpty()) {
                response.append("   Goal: ").append(s.goal).append("\n");
            }
            count++;
        }

        // Provide guidance based on next step
        if (nextStep != null) {
            switch (nextStep.toLowerCase()) {
                case "validate":
                    response.append("\n**Validation needed**: Please verify this reasoning is correct.\n");
                    break;
                case "conclude":
                    response.append("\n**Conclusion**: Based on the above reasoning.\n");
                    // Clear history after concluding
                    history.clear();
                    break;
                default:
                    response.append("\n**Next step needed**: What should I do next?\n");
            }
        }

        return response.toString();
    }

    @NotNull
    private static ThinkingStep getThinkingStep(String thought, String nextStep, String goal) {
        ThinkingStep.ThoughtType type;
        if (nextStep != null) {
            type = switch (nextStep.toLowerCase()) {
                case "validate" -> ThinkingStep.ThoughtType.VALIDATE;
                case "conclude" -> ThinkingStep.ThoughtType.CONCLUDE;
                default -> ThinkingStep.ThoughtType.THINK;
            };
        } else {
            type = ThinkingStep.ThoughtType.THINK;
        }

        // Create and store the thought
        return new ThinkingStep(thought, type, goal);
    }

    /**
     * Start a new thinking session, clearing previous thoughts.
     */
    @Tool(description = "Clear all previous thinking and start fresh. Use when switching to a new problem.")
    public String thinkNew(@ToolParam(description = "Initial thought for the new problem") String thought) {
        Deque<ThinkingStep> history = thinkingHistory.get();
        history.clear();

        ThinkingStep step = new ThinkingStep(thought, ThinkingStep.ThoughtType.THINK, null);
        history.addLast(step);

        return "## New Thinking Session\n\n1. [THINK] " + thought + "\n\nReady to reason through this problem.";
    }

    /**
     * Get a summary of the current thinking process.
     */
    @Tool(description = "Get a summary of the current thinking process.")
    public String thinkSummary() {
        Deque<ThinkingStep> history = thinkingHistory.get();

        if (history.isEmpty()) {
            return "No thinking in progress.";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("## Thinking Summary\n\n");
        summary.append("Total thoughts: ").append(history.size()).append("\n\n");

        int i = 1;
        for (ThinkingStep step : history) {
            summary.append(String.format("%d. [%s] %s\n",
                    i++, step.type.name(), step.thought));
        }

        return summary.toString();
    }

    /**
     * Ask a question to explore alternatives.
     */
    @Tool(description = "Explore alternatives or ask questions during thinking.")
    public String thinkExplore(
            @ToolParam(description = "The question or alternative to explore") String question,
            @ToolParam(description = "Options to consider (comma-separated)", required = false) String options) {

        Deque<ThinkingStep> history = thinkingHistory.get();

        String thought = "Exploring: " + question;
        if (options != null && !options.isEmpty()) {
            thought += " | Options: " + options;
        }

        ThinkingStep step = new ThinkingStep(thought, ThinkingStep.ThoughtType.THINK, question);
        history.addLast(step);

        StringBuilder response = new StringBuilder();
        response.append("## Exploring: ").append(question).append("\n\n");

        if (options != null && !options.isEmpty()) {
            response.append("Options to consider:\n");
            String[] opts = options.split(",");
            for (String opt : opts) {
                response.append("- ").append(opt.trim()).append("\n");
            }
        } else {
            response.append("What are the options? What factors should I consider?\n");
        }

        return response.toString();
    }

    /**
     * Verify the reasoning so far.
     */
    @Tool(description = "Verify the reasoning so far. Check for logical errors or gaps.")
    public String thinkVerify() {
        Deque<ThinkingStep> history = thinkingHistory.get();

        if (history.isEmpty()) {
            return "No thinking to verify.";
        }

        StringBuilder response = new StringBuilder();
        response.append("## Verification\n\n");
        response.append("Current reasoning has ").append(history.size()).append(" steps.\n\n");
        response.append("Key points:\n");

        for (ThinkingStep step : history) {
            response.append("- ").append(step.thought).append("\n");
        }

        response.append("\n**Self-check**: Does this reasoning make sense? Are there any gaps?\n");

        return response.toString();
    }

    /**
     * Clear thinking history.
     */
    @Tool(description = "Clear the thinking history.")
    public String thinkClear() {
        Deque<ThinkingStep> history = thinkingHistory.get();
        history.clear();
        return "Thinking history cleared.";
    }

    /**
         * Represents a single thinking step.
         */
        private record ThinkingStep(String thought, SequentialThinkingTool.ThinkingStep.ThoughtType type, String goal) {
            enum ThoughtType {
                THINK,
                VALIDATE,
                CONCLUDE
            }

    }
}
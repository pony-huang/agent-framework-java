package github.ponyhuang.agentframework.workflows;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.Message;

import java.util.List;
import java.util.Map;

/**
 * Interface for workflow execution.
 * A workflow defines a graph of nodes and edges.
 */
public interface Workflow {

    /**
     * Gets the name of this workflow.
     *
     * @return the workflow name
     */
    String name();

    /**
     * Gets all nodes in this workflow.
     *
     * @return list of nodes
     */
    List<Node> getNodes();

    /**
     * Gets all edges in this workflow.
     *
     * @return list of edges
     */
    List<Edge> edges();

    /**
     * Gets the start node ID.
     *
     * @return the start node ID, or null if not set
     */
    String startNodeId();

    /**
     * Executes the workflow.
     *
     * @param initialContext the initial context
     * @return the execution result
     */
    Result execute(Map<String, Object> initialContext);

    /**
     * Represents a node in the workflow.
     */
    interface Node {
        String id();
        String name();
        Agent agent();
        NodeType getType();

        enum NodeType {
            AGENT,      // Executes an agent
            CONDITION,  // Conditional branch
            PARALLEL,   // Parallel execution
            FAN_IN,     // Fan in (wait for all)
            END         // End node
        }
    }

    /**
     * Represents an edge between nodes.
     */
    interface Edge {
        String sourceId();
        String targetId();
        String condition();  // For conditional edges

        enum EdgeType {
            SEQUENTIAL,  // Sequential flow
            CONDITIONAL, // Conditional flow
            PARALLEL     // Parallel flow
        }
    }

    /**
     * Result of workflow execution.
     */
    class Result {
        private final boolean success;
        private final Map<String, Object> outputs;
        private final List<Message> messages;
        private final String error;

        private Result(Builder builder) {
            this.success = builder.success;
            this.outputs = builder.outputs;
            this.messages = builder.messages;
            this.error = builder.error;
        }

        public boolean isSuccess() {
            return success;
        }

        public Map<String, Object> getOutputs() {
            return outputs;
        }

        public List<Message> getMessages() {
            return messages;
        }

        public String getError() {
            return error;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private boolean success;
            private Map<String, Object> outputs;
            private List<Message> messages;
            private String error;

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder outputs(Map<String, Object> outputs) {
                this.outputs = outputs;
                return this;
            }

            public Builder messages(List<Message> messages) {
                this.messages = messages;
                return this;
            }

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Result build() {
                return new Result(this);
            }
        }
    }
}

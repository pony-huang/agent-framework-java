package github.ponyhuang.agentframework.workflows;

import github.ponyhuang.agentframework.agents.Agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Builder for creating Workflow instances.
 */
public class WorkflowBuilder {

    private String name = "workflow";
    private final Map<String, Workflow.Node> nodes = new LinkedHashMap<>();
    private final List<Workflow.Edge> edges = new ArrayList<>();
    private String startNodeId;

    public static WorkflowBuilder builder() {
        return new WorkflowBuilder();
    }

    /**
     * Sets the workflow name.
     *
     * @param name the workflow name
     * @return this builder
     */
    public WorkflowBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Adds an agent node to the workflow.
     *
     * @param id    the node ID
     * @param agent the agent
     * @return this builder
     */
    public WorkflowBuilder addAgentNode(String id, Agent agent) {
        nodes.put(id, new AgentNode(id, id, agent));
        if (startNodeId == null) {
            startNodeId = id;
        }
        return this;
    }

    /**
     * Adds a condition node to the workflow.
     *
     * @param id    the node ID
     * @param name the node name
     * @param condition the condition predicate
     * @return this builder
     */
    public WorkflowBuilder addConditionNode(String id, String name, Predicate<Map<String, Object>> condition) {
        nodes.put(id, new ConditionNode(id, name, condition));
        if (startNodeId == null) {
            startNodeId = id;
        }
        return this;
    }

    /**
     * Adds a parallel node to the workflow.
     *
     * @param id   the node ID
     * @param name the node name
     * @return this builder
     */
    public WorkflowBuilder addParallelNode(String id, String name) {
        nodes.put(id, new ParallelNode(id, name));
        if (startNodeId == null) {
            startNodeId = id;
        }
        return this;
    }

    /**
     * Adds an end node to the workflow.
     *
     * @param id   the node ID
     * @param name the node name
     * @return this builder
     */
    public WorkflowBuilder addEndNode(String id, String name) {
        nodes.put(id, new EndNode(id, name));
        return this;
    }

    /**
     * Adds a sequential edge between nodes.
     *
     * @param sourceId the source node ID
     * @param targetId the target node ID
     * @return this builder
     */
    public WorkflowBuilder addEdge(String sourceId, String targetId) {
        edges.add(new SequentialEdge(sourceId, targetId));
        return this;
    }

    /**
     * Adds a conditional edge between nodes.
     *
     * @param sourceId  the source node ID
     * @param targetId the target node ID
     * @param condition the condition
     * @return this builder
     */
    public WorkflowBuilder addConditionalEdge(String sourceId, String targetId, String condition) {
        edges.add(new ConditionalEdge(sourceId, targetId, condition));
        return this;
    }

    /**
     * Sets the start node.
     *
     * @param nodeId the start node ID
     * @return this builder
     */
    public WorkflowBuilder startAt(String nodeId) {
        this.startNodeId = nodeId;
        return this;
    }

    /**
     * Builds the workflow.
     *
     * @return a new Workflow instance
     */
    public Workflow build() {
        if (startNodeId == null && !nodes.isEmpty()) {
            startNodeId = nodes.keySet().iterator().next();
        }
        return new DefaultWorkflow(name, nodes, edges, startNodeId);
    }

    // Node implementations

    private record AgentNode(String id, String name, Agent agent) implements Workflow.Node {

        @Override
            public NodeType getType() {
                return NodeType.AGENT;
            }
        }

    private record ConditionNode(String id, String name,
                                 Predicate<Map<String, Object>> condition) implements Workflow.Node {

        @Override
            public Agent agent() {
                return null;
            }

            @Override
            public NodeType getType() {
                return NodeType.CONDITION;
            }
        }

    private record ParallelNode(String id, String name) implements Workflow.Node {

        @Override
            public Agent agent() {
                return null;
            }

            @Override
            public NodeType getType() {
                return NodeType.PARALLEL;
            }
        }

    private static class EndNode implements Workflow.Node {
        private final String id;
        private final String name;

        EndNode(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Agent agent() {
            return null;
        }

        @Override
        public Workflow.Node.NodeType getType() {
            return Workflow.Node.NodeType.END;
        }
    }

    // Edge implementations

    private record SequentialEdge(String sourceId, String targetId) implements Workflow.Edge {

        @Override
            public String condition() {
                return null;
            }
        }

    private record ConditionalEdge(String sourceId, String targetId, String condition) implements Workflow.Edge {
    }

    // Default Workflow implementation

    private record DefaultWorkflow(String name, Map<String, Node> nodes, List<Edge> edges,
                                   String startNodeId) implements Workflow {

        @Override
            public List<Node> getNodes() {
                return new ArrayList<>(nodes.values());
            }

            @Override
            public List<Edge> edges() {
                return new ArrayList<>(edges);
            }

            @Override
            public String startNodeId() {
                return startNodeId;
            }

            @Override
            public Result execute(Map<String, Object> initialContext) {
                WorkflowExecutor executor = new WorkflowExecutor(this);
                return executor.execute(initialContext);
            }
        }
}

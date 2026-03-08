package github.ponyhuang.agentframework.workflows;

import github.ponyhuang.agentframework.agents.Agent;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Executes workflows.
 */
public class WorkflowExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(WorkflowExecutor.class);

    private final Workflow workflow;

    public WorkflowExecutor(Workflow workflow) {
        this.workflow = workflow;
    }

    /**
     * Executes the workflow.
     *
     * @param initialContext the initial context
     * @return the result
     */
    public Workflow.Result execute(Map<String, Object> initialContext) {
        LOG.info("Workflow execution started, node count: {}", workflow.getNodes().size());
        Map<String, Object> context = new HashMap<>(initialContext);
        Map<String, Object> outputs = new HashMap<>();
        List<Message> allMessages = new ArrayList<>();

        ensureMutableMessages(context);

        try {
            String currentNodeId = findStartNode();
            Set<String> completedNodes = new HashSet<>();
            int nodeCount = 0;

            while (currentNodeId != null && !completedNodes.contains(currentNodeId)) {
                String nodeId = currentNodeId;
                Workflow.Node node = workflow.getNodes().stream()
                        .filter(n -> n.id().equals(nodeId))
                        .findFirst()
                        .orElse(null);

                if (node == null) {
                    break;
                }

                completedNodes.add(currentNodeId);
                nodeCount++;

                LOG.info("Executing workflow node: {} (type: {})", node.id(), node.getType());

                switch (node.getType()) {
                    case AGENT -> {
                        ChatResponse response = executeAgentNode(node, context, allMessages);
                        outputs.put(node.id(), response);
                        currentNodeId = findNextNode(currentNodeId, context);
                    }
                    case CONDITION -> {
                        currentNodeId = evaluateConditionNode(node, context);
                    }
                    case PARALLEL -> {
                        List<String> parallelResults = executeParallelNode(node, context, allMessages);
                        outputs.put(node.id(), parallelResults);
                        currentNodeId = findNextNode(currentNodeId, context);
                    }
                    case END -> {
                        currentNodeId = null; // End workflow
                    }
                }
            }

            LOG.info("Workflow execution completed, nodes executed: {}", nodeCount);
            return Workflow.Result.builder()
                    .success(true)
                    .outputs(outputs)
                    .messages(allMessages)
                    .build();

        } catch (Exception e) {
            LOG.error("Workflow execution failed: {}", e.getMessage());
            return Workflow.Result.builder()
                    .success(false)
                    .outputs(outputs)
                    .messages(allMessages)
                    .error(e.getMessage())
                    .build();
        }
    }

    private String findStartNode() {
        // Use the explicitly set start node if available
        String startNodeId = workflow.startNodeId();
        if (startNodeId != null) {
            return startNodeId;
        }
        // Fallback: find the first agent node
        return workflow.getNodes().stream()
                .filter(n -> n.getType() == Workflow.Node.NodeType.AGENT)
                .map(Workflow.Node::id)
                .findFirst()
                .orElse(null);
    }

    private String findNextNode(String currentNodeId, Map<String, Object> context) {
        return workflow.edges().stream()
                .filter(e -> e.sourceId().equals(currentNodeId))
                .filter(e -> e.condition() == null || evaluateEdgeCondition(e, context))
                .map(Workflow.Edge::targetId)
                .findFirst()
                .orElse(null);
    }

    private boolean evaluateEdgeCondition(Workflow.Edge edge, Map<String, Object> context) {
        String condition = edge.condition();
        if (condition == null) {
            return true;
        }
        // Simple condition evaluation
        if (condition.startsWith("$")) {
            Object value = context.get(condition.substring(1));
            return value != null && !(value instanceof Boolean && !((Boolean) value));
        }
        return true;
    }

    private ChatResponse executeAgentNode(Workflow.Node node, Map<String, Object> context, List<Message> allMessages) {
        Agent agent = node.agent();
        if (agent == null) {
            throw new IllegalStateException("Agent not found for node: " + node.id());
        }

        // Get input messages from context
        List<Message> inputMessages = extractInputMessages(context, node.id());

        // Execute agent
        List<Message> collectedMessages = agent.runStream(inputMessages).collectList().block();
        ChatResponse response = ChatResponse.builder()
                .messages(collectedMessages)
                .build();
        
        // Merge extra properties back to context if present
        if (response.getExtraProperties() != null) {
            context.putAll(response.getExtraProperties());
        }

        appendResponseMessage(response, context, allMessages);
        return response;
    }

    private List<Message> extractInputMessages(Map<String, Object> context, String nodeId) {
        Object messagesObj = context.get("messages");
        if (messagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Message> messages = (List<Message>) messagesObj;
            // Filter to only include user and system messages (exclude assistant messages from previous nodes)
            // This prevents "consecutive assistant message" errors from some API providers
            return messages.stream()
                    .filter(m -> !"assistant".equalsIgnoreCase(m.getRoleAsString()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private void ensureMutableMessages(Map<String, Object> context) {
        Object messagesObj = context.get("messages");
        if (messagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Message> messages = (List<Message>) messagesObj;
            context.put("messages", new ArrayList<>(messages));
            return;
        }
        context.put("messages", new ArrayList<Message>());
    }

    private void appendResponseMessage(ChatResponse response, Map<String, Object> context, List<Message> allMessages) {
        if (response == null) {
            return;
        }
        Message message = response.getMessage();
        if (message == null) {
            return;
        }
        synchronized (allMessages) {
            allMessages.add(message);
        }
        Object messagesObj = context.get("messages");
        if (messagesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Message> messages = (List<Message>) messagesObj;
            synchronized (messages) {
                messages.add(message);
            }
        }
    }

    private String evaluateConditionNode(Workflow.Node node, Map<String, Object> context) {
        // Find the conditional edges from this node
        return workflow.edges().stream()
                .filter(e -> e.sourceId().equals(node.id()))
                .filter(e -> e.condition() != null)
                .filter(e -> evaluateEdgeCondition(e, context))
                .map(Workflow.Edge::targetId)
                .findFirst()
                .orElse(null);
    }

    private List<String> executeParallelNode(Workflow.Node node, Map<String, Object> context, List<Message> allMessages) {
        // Find all outgoing edges from this parallel node
        List<Workflow.Edge> parallelEdges = workflow.edges().stream()
                .filter(e -> e.sourceId().equals(node.id()))
                .toList();

        // Execute all target nodes in parallel
        List<CompletableFuture<String>> futures = parallelEdges.stream()
                .map(edge -> CompletableFuture.supplyAsync(() -> {
                    String targetId = edge.targetId();
                    Workflow.Node targetNode = workflow.getNodes().stream()
                            .filter(n -> n.id().equals(targetId))
                            .findFirst()
                            .orElse(null);

                    if (targetNode != null && targetNode.getType() == Workflow.Node.NodeType.AGENT) {
                        executeAgentNode(targetNode, context, allMessages);
                        return targetId;
                    }
                    return null;
                }))
                .toList();

        // Wait for all to complete
        return futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

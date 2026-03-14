//package github.ponyhuang.agentframework.workflows;
//
//import github.ponyhuang.agentframework.agents.Agent;
//import github.ponyhuang.agentframework.providers.ChatClient;
//import github.ponyhuang.agentframework.middleware.AgentMiddleware;
//import github.ponyhuang.agentframework.sessions.AgentSession;
//import github.ponyhuang.agentframework.sessions.ContextProvider;
//import github.ponyhuang.agentframework.types.ChatResponse;
//import github.ponyhuang.agentframework.types.Message;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Unit tests for WorkflowBuilder class.
// * Tests workflow building with nodes and edges.
// */
//class WorkflowBuilderTest {
//
//    /**
//     * Test WorkflowBuilder creates empty workflow.
//     * Verifies builder creates workflow with empty nodes and edges.
//     */
//    @Test
//    void testWorkflowBuilderCreatesEmptyWorkflow() {
//        // Build empty workflow
//        Workflow workflow = WorkflowBuilder.builder().build();
//
//        // Verify empty workflow
//        assertNotNull(workflow);
//        assertTrue(workflow.getNodes().isEmpty());
//        assertTrue(workflow.edges().isEmpty());
//    }
//
//    /**
//     * Test WorkflowBuilder with agent node.
//     * Verifies agent node is added.
//     */
//    @Test
//    void testWorkflowBuilderWithAgentNode() {
//        // Create mock agent
//        Agent mockAgent = new Agent() {
//            @Override
//            public ChatResponse run(List<Message> messages, java.util.Map<String, Object> options) {
//                return null;
//            }
//
//            @Override
//            public reactor.core.publisher.Flux<Message> runStream(List<Message> messages, java.util.Map<String, Object> options) {
//                return null;
//            }
//
//            @Override
//            public String getName() {
//                return "testAgent";
//            }
//
//            @Override
//            public String getInstructions() {
//                return null;
//            }
//
//            @Override
//            public ChatClient getClient() {
//                return null;
//            }
//
//            @Override
//            public java.util.List<java.util.Map<String, Object>> getTools() {
//                return null;
//            }
//
//            @Override
//            public Agent addTool(java.util.Map<String, Object> tool) {
//                return this;
//            }
//
//            @Override
//            public Agent removeTool(String toolName) {
//                return this;
//            }
//
//            @Override
//            public Agent addContextProvider(ContextProvider provider) {
//                return this;
//            }
//
//            @Override
//            public Agent addMiddleware(AgentMiddleware middleware) {
//                return this;
//            }
//
//            @Override
//            public java.util.List<ContextProvider> getContextProviders() {
//                return java.util.List.of();
//            }
//
//            @Override
//            public java.util.List<AgentMiddleware> getMiddlewares() {
//                return java.util.List.of();
//            }
//
//            @Override
//            public AgentSession createSession() {
//                return null;
//            }
//        };
//
//        // Build workflow with agent node
//        Workflow workflow = WorkflowBuilder.builder()
//                .addAgentNode("node1", mockAgent)
//                .build();
//
//        // Verify node was added
//        assertEquals(1, workflow.getNodes().size());
//    }
//
//    /**
//     * Test WorkflowBuilder with edges.
//     * Verifies edges connect nodes.
//     */
//    @Test
//    void testWorkflowBuilderWithEdges() {
//        // Create mock agents
//        Agent mockAgent1 = createMockAgent("agent1");
//        Agent mockAgent2 = createMockAgent("agent2");
//
//        // Build workflow with nodes and edge
//        Workflow workflow = WorkflowBuilder.builder()
//                .addAgentNode("node1", mockAgent1)
//                .addAgentNode("node2", mockAgent2)
//                .addEdge("node1", "node2")
//                .build();
//
//        // Verify edges
//        assertEquals(2, workflow.getNodes().size());
//        assertEquals(1, workflow.edges().size());
//        assertEquals("node1", workflow.edges().get(0).sourceId());
//        assertEquals("node2", workflow.edges().get(0).targetId());
//    }
//
//    /**
//     * Test WorkflowBuilder with nodes only.
//     * Verifies multiple nodes can be added.
//     */
//    @Test
//    void testWorkflowBuilderWithMultipleNodes() {
//        // Create mock agents
//        Agent mockAgent1 = createMockAgent("agent1");
//        Agent mockAgent2 = createMockAgent("agent2");
//
//        // Build workflow with multiple nodes
//        Workflow workflow = WorkflowBuilder.builder()
//                .addAgentNode("start", mockAgent1)
//                .addAgentNode("end", mockAgent2)
//                .addEdge("start", "end")
//                .build();
//
//        // Verify structure
//        assertEquals(2, workflow.getNodes().size());
//        assertEquals(1, workflow.edges().size());
//    }
//
//    /**
//     * Test WorkflowBuilder fluent interface.
//     * Verifies builder can be chained.
//     */
//    @Test
//    void testWorkflowBuilderFluentInterface() {
//        // Use fluent interface
//        Workflow workflow = WorkflowBuilder.builder()
//                .addAgentNode("node1", createMockAgent("a1"))
//                .addAgentNode("node2", createMockAgent("a2"))
//                .addEdge("node1", "node2")
//                .build();
//
//        // Verify workflow was built
//        assertNotNull(workflow);
//        assertEquals(2, workflow.getNodes().size());
//    }
//
//    /**
//     * Test WorkflowBuilder default workflow name.
//     * Verifies default name is empty or provided.
//     */
//    @Test
//    void testWorkflowBuilderDefaultName() {
//        // Build workflow
//        Workflow workflow = WorkflowBuilder.builder().build();
//
//        // Verify workflow name (may be empty or have default)
//        assertNotNull(workflow.name());
//    }
//
//    /**
//     * Helper method to create mock agent.
//     */
//    private Agent createMockAgent(String name) {
//        return new Agent() {
//            @Override
//            public ChatResponse run(List<Message> messages, java.util.Map<String, Object> options) {
//                return null;
//            }
//
//            @Override
//            public reactor.core.publisher.Flux<Message> runStream(List<Message> messages, java.util.Map<String, Object> options) {
//                return null;
//            }
//
//            @Override
//            public String getName() {
//                return name;
//            }
//
//            @Override
//            public String getInstructions() {
//                return null;
//            }
//
//            @Override
//            public ChatClient getClient() {
//                return null;
//            }
//
//            @Override
//            public java.util.List<java.util.Map<String, Object>> getTools() {
//                return null;
//            }
//
//            @Override
//            public Agent addTool(java.util.Map<String, Object> tool) {
//                return this;
//            }
//
//            @Override
//            public Agent removeTool(String toolName) {
//                return this;
//            }
//
//            @Override
//            public Agent addContextProvider(ContextProvider provider) {
//                return this;
//            }
//
//            @Override
//            public Agent addMiddleware(AgentMiddleware middleware) {
//                return this;
//            }
//
//            @Override
//            public java.util.List<ContextProvider> getContextProviders() {
//                return java.util.List.of();
//            }
//
//            @Override
//            public java.util.List<AgentMiddleware> getMiddlewares() {
//                return java.util.List.of();
//            }
//
//            @Override
//            public AgentSession createSession() {
//                return null;
//            }
//        };
//    }
//}

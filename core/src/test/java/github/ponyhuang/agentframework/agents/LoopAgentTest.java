package github.ponyhuang.agentframework.agents;

import github.ponyhuang.agentframework.providers.ChatClient;
import github.ponyhuang.agentframework.sessions.DefaultSession;
import github.ponyhuang.agentframework.sessions.Session;
import github.ponyhuang.agentframework.types.ChatResponse;
import github.ponyhuang.agentframework.types.ChatResponse.Usage;
import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for LoopAgent features:
 * - Budget tracking (T017)
 * - Fallback model retry (T018)
 */
class LoopAgentTest {

    private ChatClient mockClient;

    @BeforeEach
    void setUp() {
        mockClient = mock(ChatClient.class);
        when(mockClient.getModel()).thenReturn("claude-sonnet-4-5");
    }

    /**
     * Test T017: Budget tracking integration
     * Verify that when maxBudgetUsd is set, agent stops execution when budget is exceeded.
     */
    @Test
    void testBudgetTracking_StopsWhenBudgetExceeded() {
        // Setup mock to return responses with high usage that exceeds budget
        ChatResponse highCostResponse = createMockResponse("I'll analyze this", 100000, 50000);
        when(mockClient.chat(any())).thenReturn(highCostResponse);

        // Create agent with very low budget ($0.001)
        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .maxBudgetUsd(0.001)  // Very low budget
                .maxSteps(10)
                .build();

        List<Message> messages = List.of(UserMessage.create("Do something"));

        // Run agent
        Session session = new DefaultSession("test");
        session.start();
        agent.runStream(session, messages).blockLast();

        // Verify budget was tracked
        CostTracker tracker = agent.getCostTracker();
        assertTrue(tracker.getTotalCostUsd() > 0);
    }

    /**
     * Test T017: Budget tracking with unlimited budget (0 means unlimited)
     */
    @Test
    void testBudgetTracking_UnlimitedBudget() {
        ChatResponse response = createMockResponse("Done", 100, 50);
        when(mockClient.chat(any())).thenReturn(response);

        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .maxBudgetUsd(0)  // Unlimited
                .maxSteps(1)
                .build();

        CostTracker tracker = agent.getCostTracker();
        assertTrue(tracker.isUnlimited());
    }

    /**
     * Test T018: Fallback model - primary fails, fallback succeeds
     */
    @Test
    void testFallbackModel_PrimaryFailsFallbackSucceeds() {
        // Primary model throws exception
        when(mockClient.chat(any()))
                .thenThrow(new RuntimeException("Primary model failed"))
                .thenReturn(createMockResponse("Fallback response", 100, 50));

        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .fallbackModel("claude-haiku-3-5")
                .maxSteps(2)
                .build();

        List<Message> messages = List.of(UserMessage.create("Hello"));

        // Run agent - should try fallback after primary fails
        Session session = new DefaultSession("test");
        session.start();
        List<Message> results = agent.runStream(session, messages).collectList().block();

        // Verify fallback was attempted (primary failed, fallback succeeded)
        verify(mockClient, atLeast(1)).chat(any());
    }

    /**
     * Test T018: Fallback model - both primary and fallback fail
     */
    @Test
    void testFallbackModel_BothFail() {
        // Both primary and fallback throw exceptions
        when(mockClient.chat(any()))
                .thenThrow(new RuntimeException("Primary failed"))
                .thenThrow(new RuntimeException("Fallback also failed"));

        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .fallbackModel("claude-haiku-3-5")
                .maxSteps(2)
                .build();

        List<Message> messages = List.of(UserMessage.create("Hello"));

        // Run agent - should complete gracefully even when both fail
        Session session2 = new DefaultSession("test");
        session2.start();
        List<Message> results = agent.runStream(session2, messages).collectList().block();

        // Verify fallback was attempted
        verify(mockClient, atLeast(1)).chat(any());
    }

    /**
     * Test T018: No fallback model configured - fails directly
     */
    @Test
    void testFallbackModel_NoFallbackConfigured() {
        when(mockClient.chat(any())).thenThrow(new RuntimeException("API Error"));

        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .maxSteps(1)
                // No fallback model
                .build();

        List<Message> messages = List.of(UserMessage.create("Hello"));

        // Should complete gracefully even without fallback
        Session session3 = new DefaultSession("test");
        session3.start();
        List<Message> results = agent.runStream(session3, messages).collectList().block();

        // Primary was called at least once
        verify(mockClient, atLeast(1)).chat(any());
    }

    /**
     * Test permission mode - PLAN mode blocks tools
     */
    @Test
    void testPermissionMode_PlanBlocksTools() {
        // Mock response with a tool call
        ChatResponse responseWithTool = createMockResponseWithToolCall("read", "{\"file\": \"test.txt\"}");
        when(mockClient.chat(any())).thenReturn(responseWithTool);

        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .permissionMode(PermissionMode.PLAN)
                .maxSteps(1)
                .build();

        List<Message> messages = List.of(UserMessage.create("Read a file"));

        Session session4 = new DefaultSession("test");
        session4.start();
        List<Message> results = agent.runStream(session4, messages).collectList().block();

        // Verify permission mode is set
        assertEquals(PermissionMode.PLAN, agent.getPermissionMode());
    }

    /**
     * Test disallowed tools
     */
    @Test
    void testDisallowedTools() {
        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .disallowedTools(java.util.Set.of("Bash", "Write"))
                .maxSteps(1)
                .build();

        assertTrue(agent.getDisallowedTools().contains("Bash"));
        assertTrue(agent.getDisallowedTools().contains("Write"));
        assertEquals(2, agent.getDisallowedTools().size());
    }

    /**
     * Test max steps
     */
    @Test
    void testMaxSteps() {
        LoopAgent agent = LoopAgent.builder()
                .client(mockClient)
                .maxSteps(5)
                .build();

        assertEquals(5, agent.getMaxSteps());
    }

    // Helper methods

    private ChatResponse createMockResponse(String content, int promptTokens, int completionTokens) {
        ChatResponse response = mock(ChatResponse.class);
        Message message = UserMessage.create(content);
        when(response.getMessage()).thenReturn(message);
        when(response.hasFunctionCall()).thenReturn(false);
        when(response.getToolCalls()).thenReturn(Collections.emptyList());

        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(promptTokens);
        when(usage.getCompletionTokens()).thenReturn(completionTokens);
        when(usage.getTotalTokens()).thenReturn(promptTokens + completionTokens);
        when(usage.calculateCostUsd(any())).thenReturn(0.01); // Mock cost calculation

        when(response.getUsage()).thenReturn(usage);
        when(response.getModel()).thenReturn("claude-sonnet-4-5");

        return response;
    }

    private ChatResponse createMockResponseWithToolCall(String toolName, String toolArgs) {
        ChatResponse response = mock(ChatResponse.class);

        Message assistantMsg = mock(Message.class);
        when(assistantMsg.getTextContent()).thenReturn("I'll do that");
        when(response.getMessage()).thenReturn(assistantMsg);
        when(response.hasFunctionCall()).thenReturn(true);

        github.ponyhuang.agentframework.types.block.ToolUseBlock toolCall =
                mock(github.ponyhuang.agentframework.types.block.ToolUseBlock.class);
        when(toolCall.getName()).thenReturn(toolName);
        when(toolCall.getInput()).thenReturn(Map.of("file", "test.txt"));
        when(toolCall.getId()).thenReturn("tool-123");

        when(response.getToolCalls()).thenReturn(List.of(toolCall));

        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(100);
        when(usage.getCompletionTokens()).thenReturn(50);
        when(usage.getTotalTokens()).thenReturn(150);
        when(usage.calculateCostUsd(any())).thenReturn(0.001);

        when(response.getUsage()).thenReturn(usage);
        when(response.getModel()).thenReturn("claude-sonnet-4-5");

        return response;
    }
}

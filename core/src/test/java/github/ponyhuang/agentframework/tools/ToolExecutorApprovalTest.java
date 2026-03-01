package github.ponyhuang.agentframework.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolExecutor approval handler functionality.
 */
class ToolExecutorApprovalTest {

    /**
     * Test service class with sample method.
     */
    static class TestService {
        public String hello(String name) {
            return "Hello, " + name + "!";
        }
    }

    private ToolExecutor executor;
    private TestService testService;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        executor = new ToolExecutor();
        testService = new TestService();

        // Register a tool that requires approval
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .name("hello")
                .method(method)
                .instance(testService)
                .requiresApproval(true)
                .build();
        executor.register(tool);
    }

    /**
     * Test tool execution without approval handler throws SecurityException.
     */
    @Test
    void testExecuteWithoutApprovalHandlerThrowsException() {
        // Try to execute without approval handler
        assertThrows(SecurityException.class, () ->
            executor.execute("hello", Map.of("name", "World"))
        );
    }

    /**
     * Test tool execution with approval handler that approves.
     */
    @Test
    void testExecuteWithApprovalHandlerApproves() throws NoSuchMethodException {
        // Add approval handler that always approves
        executor.approvalHandler((toolName, args) -> true);

        // Execute should succeed
        Object result = executor.execute("hello", Map.of("name", "World"));
        assertEquals("Hello, World!", result);
    }

    /**
     * Test tool execution with approval handler that rejects.
     */
    @Test
    void testExecuteWithApprovalHandlerRejects() {
        // Add approval handler that always rejects
        executor.approvalHandler((toolName, args) -> false);

        // Try to execute - should throw SecurityException
        assertThrows(SecurityException.class, () ->
            executor.execute("hello", Map.of("name", "World"))
        );
    }

    /**
     * Test tool without approval requirement executes without handler.
     */
    @Test
    void testToolWithoutApprovalDoesNotNeedHandler() throws NoSuchMethodException {
        // Register a tool that does NOT require approval
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .name("hello-no-approval")
                .method(method)
                .instance(testService)
                .requiresApproval(false)
                .build();
        executor.register(tool);

        // Execute should succeed without approval handler
        Object result = executor.execute("hello-no-approval", Map.of("name", "World"));
        assertEquals("Hello, World!", result);
    }

    /**
     * Test approval handler receives correct tool name and arguments.
     */
    @Test
    void testApprovalHandlerReceivesCorrectInfo() {
        // Track what the handler received
        final String[] receivedName = new String[1];
        final Map<String, Object>[] receivedArgs = new Map[1];

        executor.approvalHandler((toolName, args) -> {
            receivedName[0] = toolName;
            receivedArgs[0] = args;
            return true;
        });

        executor.execute("hello", Map.of("name", "TestUser", "age", 25));

        assertEquals("hello", receivedName[0]);
        assertNotNull(receivedArgs[0]);
        assertEquals("TestUser", receivedArgs[0].get("name"));
        assertEquals(25, receivedArgs[0].get("age"));
    }

    /**
     * Test approval handler with empty arguments.
     */
    @Test
    void testApprovalHandlerWithEmptyArguments() {
        executor.approvalHandler((toolName, args) -> {
            assertNotNull(args);
            return true;
        });

        // Execute with empty map
        Object result = executor.execute("hello", Map.of());
        assertEquals("Hello, null!", result);
    }

    /**
     * Test getToolSchemas does not include approval requirement.
     * (Approval is handled at execution time, not in schema)
     */
    @Test
    void testGetToolSchemasExcludesApproval() {
        var schemas = executor.getToolSchemas();
        assertEquals(1, schemas.size());
        // Schema should not contain requiresApproval
        assertFalse(schemas.get(0).containsKey("requiresApproval"));
    }
}

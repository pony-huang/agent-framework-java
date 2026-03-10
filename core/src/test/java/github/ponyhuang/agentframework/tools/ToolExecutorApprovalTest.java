package github.ponyhuang.agentframework.tools;

import github.ponyhuang.agentframework.hooks.HookEvent;
import github.ponyhuang.agentframework.hooks.HookEventBus;
import github.ponyhuang.agentframework.hooks.HookResult;
import github.ponyhuang.agentframework.hooks.events.PermissionRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolExecutor hook-based permission functionality.
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
    private HookEventBus hookEventBus;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        executor = new ToolExecutor();
        testService = new TestService();
        hookEventBus = new HookEventBus();

        // Register a tool
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .name("hello")
                .method(method)
                .instance(testService)
                .build();
        executor.register(tool);

        // Attach hook event bus
        executor.hookEventBus(hookEventBus);
    }

    /**
     * Test tool execution with permission hook that allows.
     */
    @Test
    void testExecuteWithPermissionHookAllows() {
        // Add permission hook that always allows
        hookEventBus.registerHook(HookEvent.PERMISSION_REQUEST, context -> HookResult.allow());

        // Execute should succeed - just verify it doesn't throw
        assertDoesNotThrow(() ->
            executor.execute("hello", Map.of("name", "World"))
        );
    }

    /**
     * Test tool execution with permission hook that denies.
     */
    @Test
    void testExecuteWithPermissionHookDenies() {
        // Add permission hook that denies
        hookEventBus.registerHook(HookEvent.PERMISSION_REQUEST,
            context -> HookResult.deny("Not allowed"));

        // Try to execute - should throw SecurityException
        assertThrows(SecurityException.class, () ->
            executor.execute("hello", Map.of("name", "World"))
        );
    }

    /**
     * Test tool execution without permission hook executes without permission check.
     */
    @Test
    void testExecuteWithoutPermissionHookSucceeds() {
        // Don't register any permission hook - should execute without permission check
        assertDoesNotThrow(() ->
            executor.execute("hello", Map.of("name", "World"))
        );
    }

    /**
     * Test permission hook receives correct tool name and arguments.
     */
    @Test
    void testPermissionHookReceivesCorrectInfo() {
        // Track what the hook received
        final String[] receivedName = new String[1];
        final Map<String, Object>[] receivedArgs = new Map[1];

        hookEventBus.registerHook(HookEvent.PERMISSION_REQUEST, context -> {
            PermissionRequestContext permContext = (PermissionRequestContext) context;
            receivedName[0] = permContext.getToolName();
            receivedArgs[0] = permContext.getToolInput();
            return HookResult.allow();
        });

        executor.execute("hello", Map.of("name", "TestUser", "age", 25));

        assertEquals("hello", receivedName[0]);
        assertNotNull(receivedArgs[0]);
        assertEquals("TestUser", receivedArgs[0].get("name"));
        assertEquals(25, receivedArgs[0].get("age"));
    }

    /**
     * Test permission hook with empty arguments.
     */
    @Test
    void testPermissionHookWithEmptyArguments() {
        hookEventBus.registerHook(HookEvent.PERMISSION_REQUEST, context -> {
            PermissionRequestContext permContext = (PermissionRequestContext) context;
            assertNotNull(permContext.getToolInput());
            return HookResult.allow();
        });

        // Execute with empty map - should not throw
        assertDoesNotThrow(() ->
            executor.execute("hello", Map.of())
        );
    }

    /**
     * Test getToolSchemas does not include approval requirement.
     */
    @Test
    void testGetToolSchemasExcludesApproval() {
        var schemas = executor.getToolSchemas();
        assertEquals(1, schemas.size());
        // Schema should not contain requiresApproval
        assertFalse(schemas.get(0).containsKey("requiresApproval"));
    }
}

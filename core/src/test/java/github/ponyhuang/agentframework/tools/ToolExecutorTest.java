package github.ponyhuang.agentframework.tools;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToolExecutor class.
 * Tests tool registration and execution.
 */
class ToolExecutorTest {

    /**
     * Test service class with sample methods.
     */
    static class TestService {
        public String hello(String name) {
            return "Hello, " + name + "!";
        }

        public int add(int a, int b) {
            return a + b;
        }

        public void noReturn() {
            // Does nothing
        }
    }

    private ToolExecutor executor;
    private TestService testService;

    @BeforeEach
    void setUp() {
        executor = new ToolExecutor();
        testService = new TestService();
    }

    /**
     * Test ToolExecutor.register() adds a tool.
     * Verifies tool is stored correctly.
     */
    @Test
    void testRegisterAddsTool() throws NoSuchMethodException {
        // Create and register a tool
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .name("hello")
                .method(method)
                .instance(testService)
                .build();

        executor.register(tool);

        // Verify tool count
        assertEquals(1, executor.getToolCount());
        // Verify tool is retrievable
        assertNotNull(executor.getTool("hello"));
    }

    /**
     * Test ToolExecutor.getTool() returns registered tool.
     * Verifies tool retrieval.
     */
    @Test
    void testGetToolReturnsRegisteredTool() throws NoSuchMethodException {
        // Register a tool
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .name("hello")
                .method(method)
                .instance(testService)
                .build();
        executor.register(tool);

        // Get the tool
        FunctionTool retrieved = executor.getTool("hello");

        // Verify retrieved tool
        assertNotNull(retrieved);
        assertEquals("hello", retrieved.getName());
    }

    /**
     * Test ToolExecutor.getTool() returns null for unknown tool.
     * Verifies null handling.
     */
    @Test
    void testGetToolReturnsNullForUnknown() {
        // Try to get non-existent tool
        FunctionTool result = executor.getTool("unknown");

        // Verify null is returned
        assertNull(result);
    }

    /**
     * Test ToolExecutor.hasTool() returns true for registered tool.
     * Verifies hasTool method.
     */
    @Test
    void testHasToolReturnsTrue() throws NoSuchMethodException {
        // Register a tool
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .name("hello")
                .method(method)
                .instance(testService)
                .build();
        executor.register(tool);

        // Verify hasTool returns true
        assertTrue(executor.hasTool("hello"));
    }

    /**
     * Test ToolExecutor.hasTool() returns false for unknown tool.
     * Verifies false for unknown.
     */
    @Test
    void testHasToolReturnsFalseForUnknown() {
        // Verify hasTool returns false for unknown
        assertFalse(executor.hasTool("unknown"));
    }

    /**
     * Test ToolExecutor.execute() runs registered tool.
     * Verifies tool execution.
     */
    @Test
    void testExecuteRunsRegisteredTool() throws NoSuchMethodException {
        // Register a tool
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(testService)
                .build();
        executor.register(tool);

        // Execute the tool
        Object result = executor.execute("hello", Map.of("name", "World"));

        // Verify result
        assertEquals("Hello, World!", result);
    }

    /**
     * Test ToolExecutor.execute() throws for unknown tool.
     * Verifies exception handling.
     */
    @Test
    void testExecuteThrowsForUnknownTool() {
        // Try to execute unknown tool
        assertThrows(IllegalArgumentException.class, () ->
            executor.execute("unknown", Map.of())
        );
    }

    /**
     * Test ToolExecutor.executeAll() executes multiple tools.
     * Verifies batch execution.
     */
    @Test
    void testExecuteAllExecutesMultipleTools() throws NoSuchMethodException {
        // Register two tools
        Method helloMethod = TestService.class.getMethod("hello", String.class);
        Method addMethod = TestService.class.getMethod("add", int.class, int.class);

        executor.register(FunctionTool.builder()
                .name("hello")
                .method(helloMethod)
                .instance(testService)
                .build());

        executor.register(FunctionTool.builder()
                .name("add")
                .method(addMethod)
                .instance(testService)
                .build());

        // Execute all tools
        List<Map<String, Object>> calls = List.of(
            Map.of("name", "hello", "arguments", Map.of("name", "Test")),
            Map.of("name", "add", "arguments", Map.of("a", 5, "b", 3))
        );

        List<Object> results = executor.executeAll(calls);

        // Verify results
        assertEquals(2, results.size());
        assertEquals("Hello, Test!", results.get(0));
        assertEquals(8, results.get(1));
    }

    /**
     * Test ToolExecutor.executeAll() with null input.
     * Verifies null handling.
     */
    @Test
    void testExecuteAllWithNullInput() {
        // Execute with null
        List<Object> results = executor.executeAll(null);

        // Verify empty list is returned
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    /**
     * Test ToolExecutor.unregister() removes a tool.
     * Verifies tool removal.
     */
    @Test
    void testUnregisterRemovesTool() throws NoSuchMethodException {
        // Register a tool
        Method method = TestService.class.getMethod("hello", String.class);
        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(testService)
                .build();
        executor.register(tool);

        // Verify tool is registered
        assertEquals(1, executor.getToolCount());

        // Unregister the tool
        executor.unregister("hello");

        // Verify tool is removed
        assertEquals(0, executor.getToolCount());
        assertFalse(executor.hasTool("hello"));
    }

    /**
     * Test ToolExecutor.getTools() returns all registered tools.
     * Verifies getTools method.
     */
    @Test
    void testGetToolsReturnsAllTools() throws NoSuchMethodException {
        // Register multiple tools
        Method helloMethod = TestService.class.getMethod("hello", String.class);
        Method addMethod = TestService.class.getMethod("add", int.class, int.class);

        executor.register(FunctionTool.builder()
                .name("hello")
                .method(helloMethod)
                .instance(testService)
                .build());

        executor.register(FunctionTool.builder()
                .name("add")
                .method(addMethod)
                .instance(testService)
                .build());

        // Get all tools
        List<FunctionTool> tools = executor.getTools();

        // Verify all tools are returned
        assertEquals(2, tools.size());
    }

    /**
     * Test ToolExecutor.clear() removes all tools.
     * Verifies clear method.
     */
    @Test
    void testClearRemovesAllTools() throws NoSuchMethodException {
        // Register some tools
        Method helloMethod = TestService.class.getMethod("hello", String.class);
        executor.register(FunctionTool.builder()
                .method(helloMethod)
                .instance(testService)
                .build());

        // Clear all tools
        executor.clear();

        // Verify all tools are removed
        assertEquals(0, executor.getToolCount());
    }

    /**
     * Test ToolExecutor.getToolSchemas() returns schemas for LLM.
     * Verifies schema generation.
     */
    @Test
    void testGetToolSchemasReturnsSchemas() throws NoSuchMethodException {
        // Register a tool
        Method method = TestService.class.getMethod("hello", String.class);
        executor.register(FunctionTool.builder()
                .name("hello")
                .description("Says hello")
                .method(method)
                .instance(testService)
                .build());

        // Get schemas
        List<Map<String, Object>> schemas = executor.getToolSchemas();

        // Verify schemas
        assertEquals(1, schemas.size());
        assertTrue(schemas.get(0).containsKey("name"));
        assertTrue(schemas.get(0).containsKey("parameters"));
    }

    /**
     * Test ToolExecutor.registerAll() registers multiple tools.
     * Verifies batch registration.
     */
    @Test
    void testRegisterAllRegistersMultipleTools() throws NoSuchMethodException {
        // Create tools
        Method helloMethod = TestService.class.getMethod("hello", String.class);
        Method addMethod = TestService.class.getMethod("add", int.class, int.class);

        FunctionTool helloTool = FunctionTool.builder()
                .method(helloMethod)
                .instance(testService)
                .build();

        FunctionTool addTool = FunctionTool.builder()
                .method(addMethod)
                .instance(testService)
                .build();

        // Register all at once
        executor.registerAll(List.of(helloTool, addTool));

        // Verify both are registered
        assertEquals(2, executor.getToolCount());
    }

    /**
     * Test ToolExecutor.register() with null tool.
     * Verifies null handling - tool should not be added.
     */
    @Test
    void testRegisterWithNullTool() {
        // Register null tool
        executor.register(null);

        // Verify no tool was added
        assertEquals(0, executor.getToolCount());
    }
}

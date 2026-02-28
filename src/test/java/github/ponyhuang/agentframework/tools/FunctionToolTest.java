package github.ponyhuang.agentframework.tools;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FunctionTool class.
 * Tests tool invocation with various parameter types and schema generation.
 */
class FunctionToolTest {

    /**
     * Test helper class with methods for testing.
     */
    static class TestService {
        public String greet(String name) {
            return "Hello, " + name + "!";
        }

        public int add(int a, int b) {
            return a + b;
        }

        public boolean isActive(boolean active) {
            return active;
        }

        public double calculate(double value) {
            return value * 2.0;
        }
    }

    /**
     * Test FunctionTool creation with builder.
     * Verifies builder pattern works correctly.
     */
    @Test
    void testFunctionToolBuilder() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("greet", String.class);

        FunctionTool tool = FunctionTool.builder()
                .name("greet")
                .description("Greets a user")
                .method(method)
                .instance(service)
                .build();

        // Verify tool fields
        assertEquals("greet", tool.getName());
        assertEquals("Greets a user", tool.getDescription());
    }

    /**
     * Test FunctionTool.invoke() with String parameter.
     * Verifies string parameter handling.
     */
    @Test
    void testInvokeWithStringParameter() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("greet", String.class);

        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(service)
                .build();

        // Invoke the tool
        Object result = tool.invoke(Map.of("name", "World"));

        // Verify result
        assertEquals("Hello, World!", result);
    }

    /**
     * Test FunctionTool.invoke() with int parameters.
     * Verifies integer parameter handling.
     */
    @Test
    void testInvokeWithIntParameters() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("add", int.class, int.class);

        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(service)
                .build();

        // Invoke the tool
        Object result = tool.invoke(Map.of("a", 5, "b", 3));

        // Verify result
        assertEquals(8, result);
    }

    /**
     * Test FunctionTool.invoke() with boolean parameter.
     * Verifies boolean parameter handling.
     */
    @Test
    void testInvokeWithBooleanParameter() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("isActive", boolean.class);

        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(service)
                .build();

        // Invoke with true
        Object resultTrue = tool.invoke(Map.of("active", true));
        assertEquals(true, resultTrue);

        // Invoke with false
        Object resultFalse = tool.invoke(Map.of("active", false));
        assertEquals(false, resultFalse);
    }

    /**
     * Test FunctionTool.invoke() with double parameter.
     * Verifies double parameter handling.
     */
    @Test
    void testInvokeWithDoubleParameter() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("calculate", double.class);

        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(service)
                .build();

        // Invoke the tool
        Object result = tool.invoke(Map.of("value", 5.5));

        // Verify result (double comparison)
        assertEquals(11.0, (Double) result, 0.001);
    }

    /**
     * Test FunctionTool.toSchema() generates correct schema.
     * Verifies schema generation for LLM function calling.
     */
    @Test
    void testToSchemaGeneratesCorrectSchema() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("greet", String.class);

        FunctionTool tool = FunctionTool.builder()
                .name("greet")
                .description("Greets a user")
                .method(method)
                .instance(service)
                .build();

        // Get schema
        Map<String, Object> schema = tool.toSchema();

        // Verify schema structure
        assertEquals("greet", schema.get("name"));
        assertEquals("Greets a user", schema.get("description"));
        assertNotNull(schema.get("parameters"));

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) schema.get("parameters");
        assertEquals("object", params.get("type"));
        assertNotNull(params.get("properties"));
    }

    /**
     * Test FunctionTool.getParameters() returns correct parameter info.
     * Verifies parameter metadata.
     */
    @Test
    void testGetParameters() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("greet", String.class);

        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(service)
                .build();

        // Get parameters
        Map<String, Object> params = tool.getParameters();

        // Verify parameter structure
        assertEquals("object", params.get("type"));
        assertNotNull(params.get("properties"));
        assertNotNull(params.get("required"));
    }

    /**
     * Test FunctionTool.requiresApproval() default value.
     * Verifies default requiresApproval is false.
     */
    @Test
    void testRequiresApprovalDefault() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("greet", String.class);

        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(service)
                .build();

        // Verify default is false
        assertFalse(tool.requiresApproval());
    }

    /**
     * Test FunctionTool.requiresApproval() can be set.
     * Verifies builder sets requiresApproval.
     */
    @Test
    void testRequiresApprovalCanBeSet() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("greet", String.class);

        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(service)
                .requiresApproval(true)
                .build();

        // Verify requiresApproval is true
        assertTrue(tool.requiresApproval());
    }

    /**
     * Test FunctionTool.create() factory method.
     * Verifies convenience factory method.
     */
    @Test
    void testCreateFactoryMethod() throws NoSuchMethodException {
        TestService service = new TestService();
        Method method = TestService.class.getMethod("greet", String.class);

        // Use create factory method
        FunctionTool tool = FunctionTool.create(method, service);

        // Verify tool was created
        assertNotNull(tool);
        assertEquals("greet", tool.getName());

        // Verify invocation works
        Object result = tool.invoke(Map.of("name", "Test"));
        assertEquals("Hello, Test!", result);
    }

    /**
     * Test FunctionTool throws ToolExecutionException on invocation error.
     * Verifies exception handling.
     */
    @Test
    void testInvokeThrowsExceptionOnError() throws NoSuchMethodException {
        // Create a service with a method that throws exception
        Object badService = new Object() {
            public String fail() {
                throw new RuntimeException("Intentional error");
            }
        };

        Method method = badService.getClass().getMethod("fail");
        FunctionTool tool = FunctionTool.builder()
                .method(method)
                .instance(badService)
                .build();

        // Verify exception is thrown
        assertThrows(FunctionTool.ToolExecutionException.class, () -> tool.invoke(Map.of()));
    }
}

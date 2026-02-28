package github.ponyhuang.agentframework.middleware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MiddlewareChain class.
 * Tests chain execution order and context passing.
 */
class MiddlewareChainTest {

    private MiddlewareChain<String, String> chain;
    private List<String> executionOrder;

    @BeforeEach
    void setUp() {
        chain = new MiddlewareChain<>();
        executionOrder = new ArrayList<>();
    }

    /**
     * Test MiddlewareChain.add() adds a middleware.
     * Verifies middleware is added to chain.
     */
    @Test
    void testAddAddsMiddleware() {
        // Add a middleware
        chain.add((context, next) -> {
            executionOrder.add("middleware1");
            return next.apply(context);
        });

        // Verify middleware count
        assertEquals(1, chain.getMiddlewareCount());
    }

    /**
     * Test MiddlewareChain.add() with null does nothing.
     * Verifies null handling.
     */
    @Test
    void testAddWithNullDoesNothing() {
        // Add null middleware
        chain.add(null);

        // Verify no middleware added
        assertEquals(0, chain.getMiddlewareCount());
    }

    /**
     * Test MiddlewareChain.execute() runs middlewares in order.
     * Verifies execution order (first added = first executed).
     */
    @Test
    void testExecuteRunsMiddlewaresInOrder() {
        // Add multiple middlewares
        chain.add((context, next) -> {
            executionOrder.add("1");
            return next.apply(context);
        });
        chain.add((context, next) -> {
            executionOrder.add("2");
            return next.apply(context);
        });
        chain.add((context, next) -> {
            executionOrder.add("3");
            return next.apply(context);
        });

        // Execute chain
        chain.execute("context", c -> {
            executionOrder.add("handler");
            return "result";
        });

        // Verify execution order: 1 -> 2 -> 3 -> handler
        assertEquals(4, executionOrder.size());
        assertEquals("1", executionOrder.get(0));
        assertEquals("2", executionOrder.get(1));
        assertEquals("3", executionOrder.get(2));
        assertEquals("handler", executionOrder.get(3));
    }

    /**
     * Test MiddlewareChain.execute() with empty chain calls handler.
     * Verifies handler is called when no middlewares.
     */
    @Test
    void testExecuteWithEmptyChain() {
        // Execute with empty chain
        String result = chain.execute("context", c -> {
            executionOrder.add("handler");
            return "handler_result";
        });

        // Verify handler was called
        assertEquals(1, executionOrder.size());
        assertEquals("handler", executionOrder.get(0));
        assertEquals("handler_result", result);
    }

    /**
     * Test MiddlewareChain.execute() passes context through chain.
     * Verifies context is passed correctly.
     */
    @Test
    void testExecutePassesContext() {
        // Add middleware that modifies context
        chain.add((context, next) -> {
            return "modified_" + next.apply(context);
        });

        // Execute chain
        String result = chain.execute("original", c -> c);

        // Verify context was modified
        assertEquals("modified_original", result);
    }

    /**
     * Test MiddlewareChain.isEmpty() returns true for empty chain.
     * Verifies isEmpty method.
     */
    @Test
    void testIsEmptyReturnsTrueForEmpty() {
        // Verify is empty
        assertTrue(chain.isEmpty());
    }

    /**
     * Test MiddlewareChain.isEmpty() returns false when middlewares exist.
     * Verifies isEmpty returns false.
     */
    @Test
    void testIsEmptyReturnsFalse() {
        // Add middleware
        chain.add((context, next) -> next.apply(context));

        // Verify is not empty
        assertFalse(chain.isEmpty());
    }

    /**
     * Test MiddlewareChain.clear() removes all middlewares.
     * Verifies clear method.
     */
    @Test
    void testClearRemovesAllMiddlewares() {
        // Add middlewares
        chain.add((context, next) -> next.apply(context));
        chain.add((context, next) -> next.apply(context));

        // Clear chain
        chain.clear();

        // Verify empty
        assertTrue(chain.isEmpty());
        assertEquals(0, chain.getMiddlewareCount());
    }

    /**
     * Test MiddlewareChain.addAll() adds multiple middlewares.
     * Verifies batch add.
     */
    @Test
    void testAddAllAddsMultipleMiddlewares() {
        // Create middlewares
        List<MiddlewareChain.Middleware<String, String>> middlewares = List.of(
            (context, next) -> next.apply(context),
            (context, next) -> next.apply(context)
        );

        // Add all
        chain.addAll(middlewares);

        // Verify count
        assertEquals(2, chain.getMiddlewareCount());
    }

    /**
     * Test MiddlewareChain.addAll() with null does nothing.
     * Verifies null handling.
     */
    @Test
    void testAddAllWithNullDoesNothing() {
        // Add null list
        chain.addAll(null);

        // Verify count is still 0
        assertEquals(0, chain.getMiddlewareCount());
    }

    /**
     * Test MiddlewareChain getMiddlewareCount returns correct count.
     * Verifies getMiddlewareCount method.
     */
    @Test
    void testGetMiddlewareCount() {
        // Add three middlewares
        chain.add((context, next) -> next.apply(context));
        chain.add((context, next) -> next.apply(context));
        chain.add((context, next) -> next.apply(context));

        // Verify count
        assertEquals(3, chain.getMiddlewareCount());
    }

    /**
     * Test middleware can short-circuit by not calling next.
     * Verifies early termination.
     */
    @Test
    void testMiddlewareCanShortCircuit() {
        // Add middlewares where first one short-circuits
        chain.add((context, next) -> {
            executionOrder.add("short-circuit");
            return "early_return";
        });
        chain.add((context, next) -> {
            executionOrder.add("should-not-run");
            return next.apply(context);
        });

        // Execute chain
        String result = chain.execute("context", c -> {
            executionOrder.add("handler");
            return "handler_result";
        });

        // Verify short-circuit happened
        assertEquals("early_return", result);
        assertEquals(1, executionOrder.size());
        assertEquals("short-circuit", executionOrder.get(0));
    }
}

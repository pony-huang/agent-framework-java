package github.ponyhuang.agentframework.middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Chain for executing middleware in sequence.
 *
 * @param <C> the context type
 * @param <R> the result type
 */
public class MiddlewareChain<C, R> {

    private final List<Middleware<C, R>> middlewares = new ArrayList<>();

    /**
     * Adds a middleware to the chain.
     *
     * @param middleware the middleware to add
     * @return this chain for chaining
     */
    public MiddlewareChain<C, R> add(Middleware<C, R> middleware) {
        if (middleware != null) {
            middlewares.add(middleware);
        }
        return this;
    }

    /**
     * Adds multiple middlewares to the chain.
     *
     * @param middlewares the middlewares to add
     * @return this chain for chaining
     */
    public MiddlewareChain<C, R> addAll(List<? extends Middleware<C, R>> middlewares) {
        if (middlewares != null) {
            for (Middleware<C, R> middleware : middlewares) {
                add(middleware);
            }
        }
        return this;
    }

    /**
     * Executes the chain with the given context and final handler.
     *
     * @param context the context
     * @param finalHandler the final handler if all middlewares pass through
     * @return the result
     */
    public R execute(C context, Function<C, R> finalHandler) {
        if (middlewares.isEmpty()) {
            return finalHandler.apply(context);
        }

        // Build the chain from the end
        Function<C, R> chain = finalHandler;
        for (int i = middlewares.size() - 1; i >= 0; i--) {
            final int index = i;
            final C ctx = context;
            final Function<C, R> next = chain;
            chain = c -> middlewares.get(index).process(c, next);
        }

        return chain.apply(context);
    }

    /**
     * Gets the number of middlewares in the chain.
     *
     * @return the middleware count
     */
    public int getMiddlewareCount() {
        return middlewares.size();
    }

    /**
     * Checks if the chain is empty.
     *
     * @return true if no middlewares
     */
    public boolean isEmpty() {
        return middlewares.isEmpty();
    }

    /**
     * Clears all middlewares from the chain.
     *
     * @return this chain for chaining
     */
    public MiddlewareChain<C, R> clear() {
        middlewares.clear();
        return this;
    }

    /**
     * Generic middleware interface.
     *
     * @param <C> context type
     * @param <R> result type
     */
    public interface Middleware<C, R> {
        R process(C context, Function<C, R> next);
    }
}

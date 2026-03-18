package github.ponyhuang.agentframework.sessions;

import github.ponyhuang.agentframework.types.message.Message;
import github.ponyhuang.agentframework.types.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for session fork functionality.
 */
class DefaultSessionTest {

    @Test
    void testForkCreatesIndependentCopy() {
        DefaultSession original = new DefaultSession("original-id");
        original.addMessage(UserMessage.create("Hello"));
        original.start();

        Session fork = original.fork();

        // Fork should have different ID
        assertNotEquals(original.getId(), fork.getId());

        // Fork should have parent's ID
        assertEquals(original.getId(), fork.getParentSessionId());

        // Fork should have the same messages
        assertEquals(1, fork.getMessages().size());
        assertEquals("Hello", fork.getMessages().get(0).getTextContent());
    }

    @Test
    void testForkIsIndependent() {
        DefaultSession original = new DefaultSession("original-id");
        original.addMessage(UserMessage.create("Original message"));
        original.start();

        Session fork = original.fork();

        // Add message to fork
        fork.addMessage(UserMessage.create("Fork message"));

        // Original should be unchanged
        assertEquals(1, original.getMessages().size());

        // Fork should have both messages
        assertEquals(2, fork.getMessages().size());
    }

    @Test
    void testForkPreservesMetadata() {
        DefaultSession original = new DefaultSession("original-id");
        original.setMetadata("key1", "value1");
        original.setMetadata("key2", 42);
        original.start();

        Session fork = original.fork();

        assertEquals("value1", fork.getMetadata("key1"));
        assertEquals(42, fork.getMetadata("key2"));
    }

    @Test
    void testForkIdFormat() {
        DefaultSession original = new DefaultSession("original-id");
        original.start();

        Session fork = original.fork();

        // Fork ID should contain original ID and timestamp marker
        assertTrue(fork.getId().startsWith("original-id-fork-"));
    }

    @Test
    void testOriginalSessionNoParent() {
        DefaultSession original = new DefaultSession("original-id");
        original.start();

        assertNull(original.getParentSessionId());
    }

    @Test
    void testForkInactive() {
        DefaultSession original = new DefaultSession("original-id");
        // Don't start - should still be able to fork

        Session fork = original.fork();

        assertNotNull(fork);
        // Fork inherits active state from parent at creation time
        // or starts inactive
    }
}

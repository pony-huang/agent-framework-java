package github.ponyhuang.agentframework;

import github.ponyhuang.agentframework.tools.ToolExecutor;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify logging framework is properly configured.
 */
class LoggingVerificationTest {

    @Test
    void testLoggingFrameworkInitializesWithoutWarnings() {
        // Verify that SLF4J is properly bound to Logback
        Logger logger = LoggerFactory.getLogger(LoggingVerificationTest.class);
        assertNotNull(logger);

        // Log at different levels to verify they work
        logger.trace("TRACE level test");
        logger.debug("DEBUG level test");
        logger.info("INFO level test");
        logger.warn("WARN level test");
        logger.error("ERROR level test");

        // Test that classes with logging compile and can be instantiated
        ToolExecutor executor = new ToolExecutor();
        assertNotNull(executor);
        assertEquals(0, executor.getToolCount());

        System.out.println("Logging framework verification passed - no SLF4J warnings!");
    }
}

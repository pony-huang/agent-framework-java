package github.ponyhuang.agentframework.samples.validation;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

/**
 * Runs a sample and captures its output.
 */
public class SampleRunner {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SampleResult run(SampleDefinition sample) {
        System.out.println("Running sample: " + sample.getName());
        
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        
        Instant start = Instant.now();
        boolean success = false;
        String errorMsg = "";

        try {
            // Capture output
            System.setOut(new PrintStream(outContent));
            System.setErr(new PrintStream(errContent));

            // Run in a separate thread with timeout
            Future<?> future = executor.submit(() -> {
                try {
                    Method main = sample.getSampleClass().getMethod("main", String[].class);
                    main.invoke(null, (Object) new String[]{});
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Wait for completion (e.g., 30 seconds timeout)
            // Note: HostingExample will timeout, which is expected behavior for a server
            try {
                future.get(30, TimeUnit.SECONDS);
                success = true;
            } catch (TimeoutException e) {
                future.cancel(true);
                if (sample.getName().contains("Hosting")) {
                    success = true; // Hosting examples are expected to run indefinitely
                    errorMsg = "Terminated (Timeout - Expected for Server)";
                } else {
                    errorMsg = "Timeout";
                }
            } catch (ExecutionException e) {
                errorMsg = e.getCause().toString();
            }

        } catch (Exception e) {
            errorMsg = e.toString();
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }

        Instant end = Instant.now();
        
        return new SampleResult(
                sample.getName(),
                success,
                outContent.toString(),
                errContent.toString().isEmpty() ? errorMsg : errContent.toString() + "\n" + errorMsg,
                Duration.between(start, end)
        );
    }
    
    public void shutdown() {
        executor.shutdownNow();
    }
}

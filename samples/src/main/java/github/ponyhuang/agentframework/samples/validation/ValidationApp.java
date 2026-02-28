package github.ponyhuang.agentframework.samples.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Main application for validating samples.
 */
public class ValidationApp {

    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("   Agent Framework Sample Validation");
        System.out.println("==========================================\n");

        ValidationConfig config = ValidationConfig.defaultConfig();
        
        // Simple args parsing (could be improved)
        if (args.length > 0) {
            // e.g., --subdir=observability
        }

        SampleDiscoverer discoverer = new SampleDiscoverer();
        List<SampleDefinition> samples = discoverer.discover(config);

        System.out.println("Discovered " + samples.size() + " samples in " + config.getSamplesDir() + "\n");

        SampleRunner runner = new SampleRunner();
        List<SampleResult> results = new ArrayList<>();

        // Skip samples that require specific env vars if they are missing
        // Or assume env vars are set.
        boolean hasOpenAI = System.getenv("MY_OPENAI_API_KEY") != null;
        
        if (!hasOpenAI) {
            System.out.println("WARNING: MY_OPENAI_API_KEY not found. Most samples will fail or be skipped.");
        }

        for (SampleDefinition sample : samples) {
            // Optional: Skip specific samples
            if (sample.getName().equals("HostingExample")) {
                // Skip hosting example as it blocks
                System.out.println("Skipping HostingExample (Server)");
                continue;
            }
            
            SampleResult result = runner.run(sample);
            results.add(result);
            
            System.out.println(result);
            if (!result.isSuccess()) {
                System.out.println("ERROR OUTPUT:\n" + result.getError());
            }
            System.out.println("------------------------------------------");
        }

        runner.shutdown();

        // Print Summary
        System.out.println("\n==========================================");
        System.out.println("   Validation Summary");
        System.out.println("==========================================");
        
        int passed = 0;
        for (SampleResult result : results) {
            System.out.printf("%-30s | %-5s | %d ms\n", 
                    result.getSampleName(), 
                    result.isSuccess() ? "PASS" : "FAIL", 
                    result.getDuration().toMillis());
            if (result.isSuccess()) passed++;
        }
        
        System.out.println("\nTotal: " + results.size());
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + (results.size() - passed));
        
        if (passed < results.size()) {
            System.exit(1);
        }
    }
}

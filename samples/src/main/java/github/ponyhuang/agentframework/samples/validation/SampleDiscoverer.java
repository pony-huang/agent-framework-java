package github.ponyhuang.agentframework.samples.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Discovers sample classes in the project.
 */
public class SampleDiscoverer {

    private static final String SAMPLES_PACKAGE = "com.microsoft.agentframework.samples";
    private static final String SRC_DIR = "samples/src/main/java";

    public List<SampleDefinition> discover(ValidationConfig config) {
        List<SampleDefinition> samples = new ArrayList<>();
        
        Path packagePath = config.getSamplesDir().resolve(SAMPLES_PACKAGE.replace('.', '/'));
        
        if (config.getSubdir() != null) {
            packagePath = packagePath.resolve(config.getSubdir());
        }
        
        if (!Files.exists(packagePath)) {
            System.err.println("Could not find samples directory: " + packagePath);
            return samples;
        }

        try (Stream<Path> paths = Files.walk(packagePath)) {
            List<String> classNames = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(p -> {
                        String fileName = p.getFileName().toString();
                        return SAMPLES_PACKAGE + "." + fileName.substring(0, fileName.length() - 5);
                    })
                    .collect(Collectors.toList());

            for (String className : classNames) {
                try {
                    Class<?> clazz = Class.forName(className);
                    // Check if it has a main method
                    try {
                        clazz.getMethod("main", String[].class);
                        // Filter out the validation app itself and test classes
                        if (!className.contains(".validation.") && !className.endsWith("Test")) {
                            samples.add(new SampleDefinition(clazz));
                        }
                    } catch (NoSuchMethodException e) {
                        // No main method, skip
                    }
                } catch (ClassNotFoundException e) {
                    System.err.println("Could not load class: " + className);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return samples;
    }
}

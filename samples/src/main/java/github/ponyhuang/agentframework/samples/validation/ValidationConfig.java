package github.ponyhuang.agentframework.samples.validation;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for the validation process.
 */
public class ValidationConfig {
    private final Path samplesDir;
    private final String subdir;
    private final boolean stopOnFailure;

    public ValidationConfig(String samplesDir, String subdir, boolean stopOnFailure) {
        this.samplesDir = Paths.get(samplesDir);
        this.subdir = subdir;
        this.stopOnFailure = stopOnFailure;
    }

    public Path getSamplesDir() {
        return samplesDir;
    }

    public String getSubdir() {
        return subdir;
    }

    public boolean isStopOnFailure() {
        return stopOnFailure;
    }
    
    public static ValidationConfig defaultConfig() {
        // Try to find the samples source directory
        String userDir = System.getProperty("user.dir");
        Path path = Paths.get(userDir, "samples", "src", "main", "java");
        if (!path.toFile().exists()) {
            // Maybe we are in the root directory and project structure is different?
            // Fallback to current directory if not found
            path = Paths.get(userDir);
        }
        return new ValidationConfig(path.toString(), null, false);
    }
}

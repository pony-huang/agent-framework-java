package github.ponyhuang.agentframework.samples.validation;

/**
 * Represents a sample to be validated.
 */
public class SampleDefinition {
    private final String name;
    private final Class<?> sampleClass;
    private final String description;

    public SampleDefinition(Class<?> sampleClass) {
        this.sampleClass = sampleClass;
        this.name = sampleClass.getSimpleName();
        this.description = "Sample class: " + sampleClass.getName();
    }

    public String getName() {
        return name;
    }

    public Class<?> getSampleClass() {
        return sampleClass;
    }

    public String getDescription() {
        return description;
    }
}

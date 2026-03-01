package github.ponyhuang.agentframework.types;

/**
 * Represents the role of a message sender in a conversation.
 */
public enum Role {
    /**
     * User message role.
     */
    USER("user"),

    /**
     * Assistant (AI) message role.
     */
    ASSISTANT("assistant"),

    /**
     * System message role.
     */
    SYSTEM("system"),

    /**
     * Tool result message role.
     */
    TOOL("tool");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    /**
     * Gets the string representation of the role.
     *
     * @return the role value
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts a string value to a Role enum.
     *
     * @param value the string value
     * @return the corresponding Role
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static Role fromValue(String value) {
        for (Role role : Role.values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}

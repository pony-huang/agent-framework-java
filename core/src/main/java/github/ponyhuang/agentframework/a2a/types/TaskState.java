package github.ponyhuang.agentframework.a2a.types;

public enum TaskState {
    SUBMITTED("submitted"),
    WORKING("working"),
    INPUT_REQUIRED("input_required"),
    AUTH_REQUIRED("auth_required"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELED("canceled"),
    REJECTED("rejected");

    private final String value;

    TaskState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TaskState fromValue(String value) {
        for (TaskState state : TaskState.values()) {
            if (state.value.equalsIgnoreCase(value)) {
                return state;
            }
        }
        throw new IllegalArgumentException("Unknown task state: " + value);
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELED || this == REJECTED;
    }

    public boolean isInProgress() {
        return this == SUBMITTED || this == WORKING || this == INPUT_REQUIRED || this == AUTH_REQUIRED;
    }
}

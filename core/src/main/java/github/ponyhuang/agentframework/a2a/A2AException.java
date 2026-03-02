package github.ponyhuang.agentframework.a2a;

public class A2AException extends RuntimeException {
    private final int code;

    public A2AException(String message) {
        super(message);
        this.code = -1;
    }

    public A2AException(String message, Throwable cause) {
        super(message, cause);
        this.code = -1;
    }

    public A2AException(int code, String message) {
        super(message);
        this.code = code;
    }

    public A2AException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static A2AException transportError(String message) {
        return new A2AException(-32000, "Transport error: " + message);
    }

    public static A2AException transportError(String message, Throwable cause) {
        return new A2AException(-32000, "Transport error: " + message, cause);
    }

    public static A2AException agentCardError(String message) {
        return new A2AException(-32001, "AgentCard error: " + message);
    }

    public static A2AException taskError(String message) {
        return new A2AException(-32002, "Task error: " + message);
    }
}

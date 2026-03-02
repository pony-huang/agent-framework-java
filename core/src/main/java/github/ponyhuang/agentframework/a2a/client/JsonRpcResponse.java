package github.ponyhuang.agentframework.a2a.client;

public class JsonRpcResponse {
    private final String jsonrpc;
    private final Object result;
    private final JsonRpcError error;
    private final String id;

    public JsonRpcResponse(String jsonrpc, Object result, JsonRpcError error, String id) {
        this.jsonrpc = jsonrpc;
        this.result = result;
        this.error = error;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public Object getResult() {
        return result;
    }

    public JsonRpcError getError() {
        return error;
    }

    public String getId() {
        return id;
    }

    public boolean isError() {
        return error != null;
    }

    public static class JsonRpcError {
        private final int code;
        private final String message;
        private final Object data;

        public JsonRpcError(int code, String message) {
            this(code, message, null);
        }

        public JsonRpcError(int code, String message, Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }
    }
}

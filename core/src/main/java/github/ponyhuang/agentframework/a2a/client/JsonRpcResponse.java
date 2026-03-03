package github.ponyhuang.agentframework.a2a.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRpcResponse {
    @JsonProperty("jsonrpc")
    private final String jsonrpc;
    @JsonProperty("result")
    private final Object result;
    @JsonProperty("error")
    private final JsonRpcError error;
    @JsonProperty("id")
    private final String id;

    @JsonCreator
    public JsonRpcResponse(
            @JsonProperty("jsonrpc") String jsonrpc,
            @JsonProperty("result") Object result,
            @JsonProperty("error") JsonRpcError error,
            @JsonProperty("id") String id) {
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
        @JsonProperty("code")
        private final int code;
        @JsonProperty("message")
        private final String message;
        @JsonProperty("data")
        private final Object data;

        @JsonCreator
        public JsonRpcError(
                @JsonProperty("code") int code,
                @JsonProperty("message") String message,
                @JsonProperty("data") Object data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public JsonRpcError(int code, String message) {
            this(code, message, null);
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

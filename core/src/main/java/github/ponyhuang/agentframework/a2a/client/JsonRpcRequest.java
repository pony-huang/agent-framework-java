package github.ponyhuang.agentframework.a2a.client;

import java.util.Map;

public class JsonRpcRequest {
    private final String jsonrpc = "2.0";
    private final String method;
    private final Map<String, Object> params;
    private final String id;

    public JsonRpcRequest(String method, Map<String, Object> params) {
        this.method = method;
        this.params = params;
        this.id = java.util.UUID.randomUUID().toString();
    }

    public JsonRpcRequest(String method, Map<String, Object> params, String id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getId() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String method;
        private Map<String, Object> params;
        private String id;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public JsonRpcRequest build() {
            return new JsonRpcRequest(method, params, id);
        }
    }
}

package example.agentframework.codeagent.config;

import java.util.Map;

/**
 * Configuration for MCP (Model Context Protocol) servers.
 */
public class MCPServerConfig {

    private String name;
    private String type; // "stdio" or "streamable_http"
    private String command;
    private String[] args;
    private Map<String, String> env;
    private String url;
    private Map<String, String> headers;

    public MCPServerConfig() {
    }

    public MCPServerConfig(String name, String type, String command) {
        this.name = name;
        this.type = type;
        this.command = command;
    }

    // Getters and setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
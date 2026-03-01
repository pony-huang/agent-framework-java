package github.ponyhuang.agentframework.mcp;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.net.http.HttpClient;

/**
 * MCP tool for connecting to HTTP-based MCP servers.
 * This class connects to MCP servers that communicate via streamable HTTP/SSE.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MCPStreamableHTTPTool mcpTool = MCPStreamableHTTPTool.builder()
 *     .name("web-api")
 *     .url("https://api.example.com/mcp")
 *     .description("Web API operations")
 *     .build();
 *
 * mcpTool.connect();
 * Agent agent = Agent.builder()
 *     .client(client)
 *     .tools(mcpTool)
 *     .build();
 * }</pre>
 */
public class MCPStreamableHTTPTool extends MCPTool {

    private final String url;
    private final HttpClient httpClient;

    protected MCPStreamableHTTPTool(Builder builder) {
        super(builder);
        this.url = builder.url;
        this.httpClient = builder.httpClient;
    }

    /**
     * Create a new Builder for MCPStreamableHTTPTool.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the URL of the MCP server.
     */
    public String getUrl() {
        return url;
    }

    @Override
    protected McpClientTransport createTransport() {
        HttpClientStreamableHttpTransport.Builder builder = HttpClientStreamableHttpTransport.builder(url)
                .jsonMapper(McpJsonDefaults.getMapper());

        HttpClient.Builder clientBuilder = HttpClient.newBuilder();
        if (httpClient != null) {
            applyHttpClientSettings(clientBuilder, httpClient);
        }
        if (requestTimeout != null) {
            builder.connectTimeout(requestTimeout);
        }
        builder.clientBuilder(clientBuilder);

        return builder.build();
    }

    private void applyHttpClientSettings(HttpClient.Builder target, HttpClient source) {
        if (source == null) {
            return;
        }
        source.connectTimeout().ifPresent(target::connectTimeout);
        source.executor().ifPresent(target::executor);
        target.sslContext(source.sslContext());
        target.sslParameters(source.sslParameters());
        source.authenticator().ifPresent(target::authenticator);
        source.proxy().ifPresent(target::proxy);
        source.cookieHandler().ifPresent(target::cookieHandler);
        target.followRedirects(source.followRedirects());
        target.version(source.version());
    }

    /**
     * Builder for MCPStreamableHTTPTool.
     */
    public static class Builder extends MCPTool.Builder<Builder> {
        private String url;
        private HttpClient httpClient;

        public MCPStreamableHTTPTool build() {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name is required");
            }
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("URL is required");
            }
            return new MCPStreamableHTTPTool(this);
        }

        /**
         * Set the URL of the MCP server.
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Set a custom HTTP client.
         */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }
    }
}

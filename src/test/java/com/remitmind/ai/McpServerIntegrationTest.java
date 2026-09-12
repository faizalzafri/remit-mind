package com.remitmind.ai;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpServerIntegrationTest {

    @LocalServerPort
    private int port;

    private McpSyncClient mcpClient;

    @BeforeEach
    void connect() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint("/mcp")
                .build();
        mcpClient = McpClient.sync(transport).build();
        mcpClient.initialize();
    }

    @AfterEach
    void disconnect() {
        mcpClient.closeGracefully();
    }

    @Test
    void exposesExistingToolMethodsOverMcpUnchanged() {
        // Given the app is running with its MCP server enabled, exposing the same
        // @Tool methods RemittanceCopilotService already calls locally

        // When an external MCP client asks what tools are available
        McpSchema.ListToolsResult result = mcpClient.listTools();

        // Then both tools show up under their real Java method names, with no
        // code written specifically for MCP
        List<String> toolNames = result.tools().stream().map(McpSchema.Tool::name).toList();
        assertThat(toolNames).contains("getExchangeRate", "getCountryCompliance");
    }

    @Test
    void callingCountryComplianceToolOverMcpReturnsTheRealStaticDataset() {
        // Given the MCP client is connected

        // When it calls getCountryCompliance the same way any external MCP client would
        McpSchema.CallToolResult result = mcpClient.callTool(McpSchema.CallToolRequest.builder("getCountryCompliance")
                .arguments(Map.of("countryName", "Mexico"))
                .build());

        // Then it gets the real embedded-dataset result back, not an error
        assertThat(result.isError()).isNotEqualTo(Boolean.TRUE);
        String text = ((McpSchema.TextContent) result.content().get(0)).text();
        assertThat(text).contains("5000").contains("MXN");
    }
}

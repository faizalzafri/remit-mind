package com.remitmind.ai;

import com.remitmind.ai.service.RemittanceCopilotService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class McpClientIntegrationTest {

    @Autowired
    private RemittanceCopilotService copilotService;

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void chatCanUseTheFilesystemMcpServerToListSandboxFiles() {
        // Given the copilot's chat() has the Filesystem MCP server's tools
        // available (spring.ai.mcp.client.stdio.connections.filesystem),
        // discovered at connection time, not written as a local @Tool method

        // When asked to list the files it has access to
        String response = copilotService.chat("mcp-fs-session-" + UUID.randomUUID(),
                "List the files in your current working directory using your file tools, and tell me their names.");

        // Then it actually calls the MCP tool and reports the real file that
        // exists in the sandboxed directory, not a guess
        assertThat(response.toLowerCase()).contains("readme");
    }
}

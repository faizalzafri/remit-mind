# MCP Filesystem Sandbox

This directory is the sandboxed root exposed to RemitMind's copilot via the
Filesystem MCP server. Only this directory (and its subdirectories) are 
readable/writable by the model through that tool - verifying that boundary 
is part of what McpFilesystemIntegrationTest checks.

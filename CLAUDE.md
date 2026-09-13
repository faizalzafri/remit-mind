# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

RemitMind is a Spring AI learning project: an AI-powered, compliance-aware remittance copilot that parses natural-language transfer requests into structured `Transaction`/`RiskAuditReport` data using Gemini. The primary purpose is to learn Spring AI by building a real (if deliberately small) application — see `Spring AI Learning Agent.md` at the repo root.

**Read `Spring AI Learning Agent.md` before doing substantive work here.** It is the operating charter for this repo, not generic advice: it defines a teach-then-build workflow, an incremental milestone curriculum (ChatClient → prompts → structured output → advisors → memory → RAG → tools → agents → eval → observability → MCP), a "don't overengineer" constraint (no Kafka/Redis/microservices/extra DBs without a concrete reason), and a requirement to keep `docs/learning/*.md` current whenever a capability is added. Follow it when the user is in a learning/building mode; a one-off bugfix doesn't need the full ceremony.

## Commands

```bash
# Run the app (needs GEMINI_API_KEY set)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run one test class
./mvnw test -Dtest=RemitMindApplicationTests

# Run one test method
./mvnw test -Dtest=RemitMindApplicationTests#testPromptInjectionBlocked

# Compile only
./mvnw compile
```

Tests requiring a live *chat* model call are annotated `@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", ...)` and are skipped automatically when the key isn't set. This does not make the rest of the suite offline: any `@SpringBootTest` (including `contextLoads` and `testPromptInjectionBlocked`) boots the full Spring context, which runs `ComplianceDocumentIngestionService`'s `CommandLineRunner` and calls the **embedding** model (a separate quota from chat) to re-ingest the compliance rulebook on every context start. The only test class with zero Gemini usage is `ComplianceDocumentIngestionServiceTest`, which has no `@SpringBootTest` and calls `loadAndSplit(...)` directly. When Gemini quota is tight, run that class alone rather than anything annotated `@SpringBootTest`.

## Architecture

Single Spring Boot module, package `com.remitmind.ai`:

- `config/AiConfig` — builds the single `ChatClient` bean: loads the system prompt from `resources/prompts/system-prompt.st` (a template with a `{currentDate}` placeholder), attaches `SimpleLoggerAdvisor` as a default advisor, and defines the `ChatMemory` bean (`MessageWindowChatMemory` over `InMemoryChatMemoryRepository`, capped at 20 messages).
- `config/PromptGuardrailAdvisor`, `config/RequestTraceIdAdvisor` — standalone `CallAdvisor` implementations, *not* registered as defaults on the `ChatClient` bean. They're added per-request in `RemittanceCopilotService`. `getOrder()` controls sequencing: `PromptGuardrailAdvisor` runs at `-100` so it can throw `SecurityException` before the request ever reaches the model.
- `service/RemittanceCopilotService` — the only place advisors, tools, and the system prompt's `currentDate` param are wired together per call. Two entry points with different pipelines:
  - `chat(sessionId, message)` → guardrail + trace advisors + `MessageChatMemoryAdvisor` (keyed on `ChatMemory.CONVERSATION_ID` = sessionId) + both tools → plain text via `.call().content()`.
  - `parse(message)` → guardrail + trace advisors + both tools, **no memory** → structured via `.call().entity(CopilotResponse.class)`. `CopilotResponse` bundles free text (`chatResponse`) plus the extracted `Transaction` and computed `RiskAuditReport` as one schema-constrained model call.
- `service/ExchangeRateTool`, `service/CountryDataTool` — `@Tool`-annotated methods registered per-call via `.tools(...)` (not bean-global). `ExchangeRateTool` wraps a `RestClient` (1.5s connect/read timeout) against Frankfurter for live FX rates and falls back to a hardcoded rate on failure rather than propagating the exception into the chat call. `CountryDataTool` used to call RestCountries the same way, but that API now requires a paid auth key (discovered when it started failing every call) — it was replaced with a small embedded static dataset (region/subregion/currency for ~18 countries), so it no longer makes a network call or has a failure mode to fall back from.
- `domain/` — `Transaction`, `RiskAuditReport`, `CopilotResponse`, `CountryComplianceInfo` are plain immutable records; these are the schemas the model's structured output is constrained to.
- `controller/RemittanceController` — `POST /api/copilot/chat` (conversational, memory-backed) and `POST /api/copilot/parse` (stateless, structured) under `/api/copilot`.
- `AiConfig.remittanceMcpToolCallbacks` — wraps the same `ExchangeRateTool`/`CountryDataTool` beans in `MethodToolCallbackProvider`, which `spring-ai-starter-mcp-server-webmvc`'s auto-configuration picks up automatically and exposes over MCP (Streamable HTTP) at `/mcp`. This is a second, independent entry point into the same tool methods — it doesn't change how `RemittanceCopilotService` calls them locally. `spring.ai.mcp.server.protocol: STREAMABLE` must be set explicitly in `application.yaml`; the property class's Java-level default does *not* satisfy the `@ConditionalOnProperty` that maps the HTTP route (verified the hard way — see `MISTAKES.md`).
- `spring.ai.mcp.client.stdio.connections.filesystem` spawns the Filesystem MCP reference server (`npx.cmd -y @modelcontextprotocol/server-filesystem ./mcp-filesystem-sandbox`) over STDIO, sandboxed to that one directory. Spring AI auto-configures its discovered tools into a `ToolCallbackProvider` bean named `mcpToolCallbacks` — a *different* bean from `remittanceMcpToolCallbacks` above (one consumes external tools, one exposes local ones), injected into `RemittanceCopilotService.chat()` only (not `parse()`) via `@Qualifier("mcpToolCallbacks")` to disambiguate the two same-typed beans. `npx.cmd`, not `npx`, is required on Windows — `ProcessBuilder`/`CreateProcess` won't resolve the bare `.cmd`-less name (see `MISTAKES.md`).

The compliance rule logic (max transfer limits, risk flagging) is described in `resources/prompts/system-prompt.st` and left to the model to apply using the numbers `CountryDataTool` returns — it is not enforced separately in Java.

Config: `spring.ai.google.genai.api-key` is bound from the `GEMINI_API_KEY` env var in `application.yaml`; model/temperature are also set there (`spring.ai.google.genai.chat.options.*`). Spring Boot and Spring AI versions are pinned in `pom.xml` (`spring-ai-bom`) — check the API exists in that pinned version before using it.

Both `spring-ai-starter-model-google-genai` and `spring-ai-starter-model-anthropic` are on the classpath. Which one auto-configures the `ChatModel` (and therefore the `ChatClient.Builder` that `AiConfig.chatClient()` consumes) is controlled entirely by the `spring.ai.model.chat` property (bound from `AI_CHAT_PROVIDER` env var, default `google-genai`) — see ADR-012. No Java code branches on provider; `AiConfig`/`RemittanceCopilotService` only ever depend on the provider-agnostic `ChatClient`/`ChatModel` types. The embedding model stays Google GenAI-only regardless (Anthropic has no embedding API) — this switch only affects chat.

## Learning docs (`docs/learning/`)

Maintained per `Spring AI Learning Agent.md` §25–34. `ROADMAP.md` and `PROGRESS.md` track milestone/concept status (currently: milestones 1–6 done through tool calling; RAG, agentic workflow, eval, observability, MCP not started). `CONCEPTS.md` is the concept reference notebook. `ARCHITECTURE.md` and `DECISIONS.md` should be updated whenever a new capability lands here — check them before assuming they reflect current state, they lag behind the code at times.

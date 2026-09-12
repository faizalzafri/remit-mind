<div align="center">

# 💸 RemitMind Copilot

[![Java Version](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](#)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](#)
[![Google Gemini](https://img.shields.io/badge/Gemini-3.5%20Flash-8E75C2?style=for-the-badge&logo=googlegemini&logoColor=white)](#)
[![Ollama](https://img.shields.io/badge/Ollama-qwen2.5%3A7b-000000?style=for-the-badge&logo=ollama&logoColor=white)](https://ollama.com)
[![Build Status](https://img.shields.io/badge/Build-Passing-44cc11?style=for-the-badge&logo=github&logoColor=white)](#)

An AI-powered, compliance-aware remittance copilot designed to parse, validate, and audit international money transfers.

</div>

---

## 🌟 Features

* **Natural Language Intent Parsing:** Converts casual text (e.g., *"Draft a transfer of 150 USD from Alice to Bob in Mexico for family support"*) into immutable, type-safe Java `record` transaction objects.
* **Relative Calendar Date Resolution:** System prompts load calendar dates dynamically at request-time to resolve query relative dates (e.g., *"transfer tomorrow"*) calendar-accurately.
* **Cross-Cutting Pipeline Advisors:** Implements Spring AI's Advisor pattern to cleanly separate interceptor logic:
  * `PromptGuardrailAdvisor` scans prompts and intercepts injection attempts *before* calling the LLM, protecting API budgets.
  * `RequestTraceIdAdvisor` injects transaction-correlation UUIDs and logs execution latency.
* **Multi-Turn Conversational Memory:** Uses `MessageWindowChatMemory` sliding windows and `MessageChatMemoryAdvisor` to maintain conversational context (e.g. resolving pronouns: *"Actually, make that $150"*).
* **Declarative Function Calling (Tools):** Exposes Java helper services directly to Gemini as JSON Schema schemas using `@Tool`:
  * `ExchangeRateTool` fetches live exchange rates from the Frankfurter FX API.
  * `CountryDataTool` queries the RestCountries API to audit transfer sizes against country-specific corridor compliance limits.

---

## 🛠️ Architecture

The copilot decouples core AI business rules from supporting middleware interceptors and live external API lookup tools:

```
                  [User Request (POST /api/copilot/chat)]
                                     │
                                     ▼
                     [Advisors Pipeline Interception]
        ┌────────────────────────────┼────────────────────────────┐
        ▼                            ▼                            ▼
[SimpleLoggerAdvisor]     [PromptGuardrailAdvisor]    [RequestTraceIdAdvisor]
(Default Logging)           (Prompt Safety Check)       (UUID & Latency Timing)
        │                            │                            │
        └────────────────────────────┼────────────────────────────┘
                                     │
                                     ▼
                     [MessageChatMemoryAdvisor (RAG)]
              Reads/Writes Session Context in ChatMemory
                                     │
                                     ▼
                       [Spring AI ChatClient (Gemini)]
        ┌────────────────────────────┴────────────────────────────┐
        ▼                                                         ▼
[ExchangeRateTool]                                        [CountryDataTool]
Queries Frankfurter FX API                               Queries RestCountries API
```

---

## 🚀 Getting Started

### Prerequisites
* **Java 21** (or higher)
* **Maven** (via packaged `./mvnw` wrapper)
* **Ollama** — chat and embeddings run locally by default, no API key needed (see setup below).
  A **Google Gemini API Key** is only needed if you switch providers:

  Bash:
  ```bash
  export GEMINI_API_KEY="your-gemini-api-key"
  ```
  Windows (cmd):
  ```cmd
  set GEMINI_API_KEY=your-gemini-api-key
  ```
  Windows (PowerShell):
  ```powershell
  $env:GEMINI_API_KEY = "your-gemini-api-key"
  ```

### Running the Application
To build and start the Spring Boot dev server locally:

Bash:
```bash
./mvnw spring-boot:run
```
Windows (cmd):
```cmd
mvnw.cmd spring-boot:run
```
Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```
The endpoints will be exposed at `http://localhost:8080`.

---

## 🦙 Ollama Setup (default provider)

Chat and embeddings both run locally through [Ollama](https://ollama.com) by default — no API key required.

**1. Install Ollama**

Windows (cmd or PowerShell):
```cmd
winget install --id Ollama.Ollama
```
macOS / Linux (bash):
```bash
curl -fsSL https://ollama.com/install.sh | sh
```
It starts automatically as a background service on `http://localhost:11434`.

**2. Pull the models**

Same command in any shell:
```bash
ollama pull qwen2.5:7b         # chat model
ollama pull nomic-embed-text   # embedding model
```

**3. Run**

Bash:
```bash
./mvnw spring-boot:run
```
Windows (cmd):
```cmd
mvnw.cmd spring-boot:run
```
Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

To use a different chat model, override `OLLAMA_CHAT_MODEL` (must be a model you've pulled):

Bash:
```bash
export OLLAMA_CHAT_MODEL="llama3.2"
```
Windows (cmd):
```cmd
set OLLAMA_CHAT_MODEL=llama3.2
```
Windows (PowerShell):
```powershell
$env:OLLAMA_CHAT_MODEL = "llama3.2"
```

To switch back to Gemini instead of Ollama:

Bash:
```bash
export AI_CHAT_PROVIDER=google-genai
export AI_EMBEDDING_PROVIDER=google-genai
export GEMINI_API_KEY="your-gemini-api-key"
```
Windows (cmd):
```cmd
set AI_CHAT_PROVIDER=google-genai
set AI_EMBEDDING_PROVIDER=google-genai
set GEMINI_API_KEY=your-gemini-api-key
```
Windows (PowerShell):
```powershell
$env:AI_CHAT_PROVIDER = "google-genai"
$env:AI_EMBEDDING_PROVIDER = "google-genai"
$env:GEMINI_API_KEY = "your-gemini-api-key"
```

---

## 🔌 MCP (Model Context Protocol)

RemitMind both **exposes** tools over MCP (an MCP server) and **consumes** an external MCP server (an MCP client).

### MCP Server — exposes `ExchangeRateTool` / `CountryDataTool`

Runs automatically with the app at `POST http://localhost:8080/mcp` (Streamable HTTP) — no extra setup needed.

**Test it — automated:**

Bash:
```bash
./mvnw test -Dtest=McpServerIntegrationTest
```
Windows (cmd):
```cmd
mvnw.cmd test -Dtest=McpServerIntegrationTest
```
Windows (PowerShell):
```powershell
.\mvnw.cmd test -Dtest=McpServerIntegrationTest
```

**Test it — interactively**, with the official MCP Inspector (same command any shell, once the app is already running):
```
npx @modelcontextprotocol/inspector
```
In the Inspector UI: Transport type **Streamable HTTP**, URL `http://localhost:8080/mcp`, **Connect**, then check the **Tools** tab for `getExchangeRate` / `getCountryCompliance`.

### MCP Client — consumes the Filesystem MCP server

`chat()` (not `parse()`) also has access to the official [Filesystem MCP reference server](https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem), sandboxed to the `mcp-filesystem-sandbox/` directory, spawned automatically over STDIO when the app starts. Requires Node.js (`npx`) on your `PATH` — no separate install step, `npx -y` fetches the server on first use.

**Windows note:** the configured command is `npx.cmd`, not `npx` — Java's `ProcessBuilder` can't resolve a bare `npx` on Windows (see `docs/learning/MISTAKES.md`). On macOS/Linux, override it:

Bash:
```bash
export MCP_FILESYSTEM_COMMAND=npx
```
Windows (cmd):
```cmd
set MCP_FILESYSTEM_COMMAND=npx.cmd
```
Windows (PowerShell):
```powershell
$env:MCP_FILESYSTEM_COMMAND = "npx.cmd"
```

**Test it — automated** (asks the real copilot to list its sandboxed files):

Bash:
```bash
./mvnw test -Dtest=McpClientIntegrationTest
```
Windows (cmd):
```cmd
mvnw.cmd test -Dtest=McpClientIntegrationTest
```
Windows (PowerShell):
```powershell
.\mvnw.cmd test -Dtest=McpClientIntegrationTest
```

**Try it live**, once the app is running — send a chat message asking it to list or read files in its sandbox (e.g. via the chat widget at `http://localhost:8080`, or `POST /api/copilot/chat`).

---

## 🧪 Testing

The repository contains full integration tests that exercise advisor pipelines, tool-calling networks, and conversation session persistence. 

To run the test suites:

Bash:
```bash
./mvnw test
```
Windows (cmd):
```cmd
mvnw.cmd test
```
Windows (PowerShell):
```powershell
.\mvnw.cmd test
```

*Note: Live integration tests will run if a `GEMINI_API_KEY` is present. If the environment variable is absent, integration tests are safely bypassed while offline unit tests verify the advisors and compilation.*

---

## 📜 License
This project is licensed under the [MIT License](LICENSE).

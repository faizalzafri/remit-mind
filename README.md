<div align="center">

# 💸 RemitMind Copilot

[![Java Version](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](#)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](#)
[![Google Gemini](https://img.shields.io/badge/Gemini-3.5%20Flash-8E75C2?style=for-the-badge&logo=googlegemini&logoColor=white)](#)
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
* **Google Gemini API Key** configured in your environment variables:
  ```bash
  export GEMINI_API_KEY="your-gemini-api-key"
  ```

### Running the Application
To build and start the Spring Boot dev server locally:
```bash
./mvnw spring-boot:run
```
The endpoints will be exposed at `http://localhost:8080`.

---

## 🧪 Testing

The repository contains full integration tests that exercise advisor pipelines, tool-calling networks, and conversation session persistence. 

To run the test suites:
```bash
./mvnw test
```

*Note: Live integration tests will run if a `GEMINI_API_KEY` is present. If the environment variable is absent, integration tests are safely bypassed while offline unit tests verify the advisors and compilation.*

---

## 📜 License
This project is licensed under the [MIT License](LICENSE).

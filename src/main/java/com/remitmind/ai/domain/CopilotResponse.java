package com.remitmind.ai.domain;

/**
 * Immutable record combining conversational LLM responses with extracted transaction parameters.
 */
public record CopilotResponse(
    String chatResponse,
    Transaction parsedTransaction,
    RiskAuditReport auditReport
) {}

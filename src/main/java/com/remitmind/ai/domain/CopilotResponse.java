package com.remitmind.ai.domain;

/**
 * The copilot's reply, plus the transfer it extracted and its compliance check.
 */
public record CopilotResponse(
    String chatResponse,
    Transaction parsedTransaction,
    RiskAuditReport auditReport
) {}

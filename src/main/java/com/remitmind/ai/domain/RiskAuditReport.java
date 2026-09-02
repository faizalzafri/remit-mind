package com.remitmind.ai.domain;

import java.util.List;

/**
 * Result of a compliance check on a transfer.
 */
public record RiskAuditReport(
    String status,          // APPROVED, REJECTED, or FLAG_MANUAL_REVIEW
    String riskLevel,       // LOW, MEDIUM, or HIGH
    String rationale,       // Why this status and risk level were chosen
    List<String> requiredDocuments // Documents needed before the transfer can proceed
) {}

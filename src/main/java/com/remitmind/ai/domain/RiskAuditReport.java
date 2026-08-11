package com.remitmind.ai.domain;

import java.util.List;

/**
 * Immutable record representing compliance screening and risk audit logic.
 */
public record RiskAuditReport(
    String status,          // APPROVED, REJECTED, FLAG_MANUAL_REVIEW
    String riskLevel,       // LOW, MEDIUM, HIGH
    String rationale,       // Detailed compliance explanation
    List<String> requiredDocuments // e.g. "ID Card", "Proof of Funds"
) {}

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
) {

    /**
     * Decides status/riskLevel/requiredDocuments from the corridor limit, not
     * the model. Fails closed (FLAG_MANUAL_REVIEW) if there's no compliance data.
     */
    public static RiskAuditReport evaluate(double sourceAmount, CountryComplianceInfo compliance) {
        if (compliance == null) {
            return new RiskAuditReport(
                    "FLAG_MANUAL_REVIEW",
                    "HIGH",
                    "No compliance data available for the destination country; flagged for manual review.",
                    List.of("Proof of Funds", "ID Card"));
        }
        if (sourceAmount > compliance.maxTransferLimit()) {
            return new RiskAuditReport(
                    "FLAG_MANUAL_REVIEW",
                    "MEDIUM",
                    "Amount %.2f exceeds the %.2f corridor limit for %s.".formatted(
                            sourceAmount, compliance.maxTransferLimit(), compliance.countryName()),
                    List.of("Proof of Funds", "ID Card"));
        }
        return new RiskAuditReport(
                "APPROVED",
                "LOW",
                "Amount %.2f is within the %.2f corridor limit for %s.".formatted(
                        sourceAmount, compliance.maxTransferLimit(), compliance.countryName()),
                List.of());
    }
}

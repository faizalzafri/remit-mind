package com.remitmind.ai.domain;

import java.util.List;

/**
 * Result of a compliance check on a transfer.
 */
public record RiskAuditReport(
    String status,          // APPROVED, REJECTED, or FLAG_MANUAL_REVIEW
    String riskLevel,       // LOW, MEDIUM, or HIGH
    String rationale,       // Why this status and risk level were chosen
    List<String> requiredDocuments, // Documents needed before the transfer can proceed
    boolean sanctionsHit,   // True when sanctions screening found a match
    String matchedEntity    // Name of the matched sanctions/PEP entry, null if no hit
) {

    /**
     * Decides status/riskLevel/requiredDocuments from the corridor limit, not
     * the model. Fails closed (FLAG_MANUAL_REVIEW) if there's no compliance data
     * or sanctions screening was unavailable.
     */
    public static RiskAuditReport evaluate(double sourceAmount, CountryComplianceInfo compliance,
                                           ScreeningResult screening) {
        boolean hit = screening.screeningOutcome() == ScreeningOutcome.HIT;
        String matchedEntity = screening.matchedEntity();

        if (screening.screeningOutcome() == ScreeningOutcome.UNAVAILABLE) {
            return new RiskAuditReport(
                    "FLAG_MANUAL_REVIEW",
                    "HIGH",
                    "Sanctions screening was unavailable; flagged for manual review",
                    List.of("Proof of Funds", "ID Card"),
                    hit, matchedEntity);
        }
        if (compliance == null) {
            return new RiskAuditReport(
                    "FLAG_MANUAL_REVIEW",
                    "HIGH",
                    "No compliance data available for the destination country; flagged for manual review.",
                    List.of("Proof of Funds", "ID Card"),
                    hit, matchedEntity);
        }
        if (sourceAmount > compliance.maxTransferLimit()) {
            return new RiskAuditReport(
                    "FLAG_MANUAL_REVIEW",
                    "MEDIUM",
                    "Amount %.2f exceeds the %.2f corridor limit for %s.".formatted(
                            sourceAmount, compliance.maxTransferLimit(), compliance.countryName()),
                    List.of("Proof of Funds", "ID Card"),
                    hit, matchedEntity);
        }
        return new RiskAuditReport(
                "APPROVED",
                "LOW",
                "Amount %.2f is within the %.2f corridor limit for %s.".formatted(
                        sourceAmount, compliance.maxTransferLimit(), compliance.countryName()),
                List.of(),
                hit, matchedEntity);
    }
}

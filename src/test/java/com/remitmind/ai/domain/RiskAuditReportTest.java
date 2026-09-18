package com.remitmind.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiskAuditReportTest {

    private static final CountryComplianceInfo MEXICO =
            new CountryComplianceInfo("Mexico", "MXN", "Americas", 5000.0, "Simplified declaration corridor.");

    @Test
    void approvesAmountAtTheLimit() {
        // Given an amount exactly at the corridor limit (inclusive boundary)

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(5000.0, MEXICO);

        // Then it's approved, not flagged
        assertThat(report.status()).isEqualTo("APPROVED");
        assertThat(report.riskLevel()).isEqualTo("LOW");
        assertThat(report.requiredDocuments()).isEmpty();
    }

    @Test
    void flagsAmountOverTheLimit() {
        // Given an amount one unit over the corridor limit

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(5000.01, MEXICO);

        // Then it's flagged for manual review with the required documents
        assertThat(report.status()).isEqualTo("FLAG_MANUAL_REVIEW");
        assertThat(report.riskLevel()).isEqualTo("MEDIUM");
        assertThat(report.requiredDocuments()).containsExactly("Proof of Funds", "ID Card");
    }

    @Test
    void failsClosedWhenNoComplianceDataExists() {
        // Given no compliance data for the destination country

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(100.0, null);

        // Then it's flagged for manual review, never silently approved
        assertThat(report.status()).isEqualTo("FLAG_MANUAL_REVIEW");
        assertThat(report.status()).isNotEqualTo("APPROVED");
    }

    @Test
    void isDeterministicForTheSameInputs() {
        // Given the same amount and compliance data evaluated twice

        // When evaluated
        RiskAuditReport first = RiskAuditReport.evaluate(6000.0, MEXICO);
        RiskAuditReport second = RiskAuditReport.evaluate(6000.0, MEXICO);

        // Then both results are identical
        assertThat(first).isEqualTo(second);
    }
}

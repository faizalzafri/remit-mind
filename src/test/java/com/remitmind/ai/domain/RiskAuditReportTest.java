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
        RiskAuditReport report = RiskAuditReport.evaluate(5000.0, MEXICO, ScreeningResult.CLEAR);

        // Then it's approved, not flagged
        assertThat(report.status()).isEqualTo("APPROVED");
        assertThat(report.riskLevel()).isEqualTo("LOW");
        assertThat(report.requiredDocuments()).isEmpty();
    }

    @Test
    void flagsAmountOverTheLimit() {
        // Given an amount one unit over the corridor limit

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(5000.01, MEXICO, ScreeningResult.CLEAR);

        // Then it's flagged for manual review with the required documents
        assertThat(report.status()).isEqualTo("FLAG_MANUAL_REVIEW");
        assertThat(report.riskLevel()).isEqualTo("MEDIUM");
        assertThat(report.requiredDocuments()).containsExactly("Proof of Funds", "ID Card");
    }

    @Test
    void failsClosedWhenNoComplianceDataExists() {
        // Given no compliance data for the destination country

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(100.0, null, ScreeningResult.CLEAR);

        // Then it's flagged for manual review, never silently approved
        assertThat(report.status()).isEqualTo("FLAG_MANUAL_REVIEW");
        assertThat(report.status()).isNotEqualTo("APPROVED");
    }

    @Test
    void isDeterministicForTheSameInputs() {
        // Given the same amount and compliance data evaluated twice

        // When evaluated
        RiskAuditReport first = RiskAuditReport.evaluate(6000.0, MEXICO, ScreeningResult.CLEAR);
        RiskAuditReport second = RiskAuditReport.evaluate(6000.0, MEXICO, ScreeningResult.CLEAR);

        // Then both results are identical
        assertThat(first).isEqualTo(second);
    }

    @Test
    void failCloseWhenScreeningUnavailable() {
        // Given a small amount that would normally be approved, but screening failed

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(100.0, MEXICO, new ScreeningResult(ScreeningOutcome.UNAVAILABLE, null));

        // Then its flagged, never approved
        assertThat(report.status()).isEqualTo("FLAG_MANUAL_REVIEW");
        assertThat(report.riskLevel()).isEqualTo("HIGH");
    }

    @Test
    void recordsSanctionsHitAndMatchedEntity() {
        // Given screening matched a sanctioned entity

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(100.0, MEXICO,
                new ScreeningResult(ScreeningOutcome.HIT, "Viktor Anatolijevitch BOUT"));

        // Then the hit is recorded as structured fields
        assertThat(report.sanctionsHit()).isTrue();
        assertThat(report.matchedEntity()).isEqualTo("Viktor Anatolijevitch BOUT");
    }

    @Test
    void recordsNoHitWhenScreeningIsClear() {
        // Given screening found nothing

        // When evaluated
        RiskAuditReport report = RiskAuditReport.evaluate(100.0, MEXICO, ScreeningResult.CLEAR);

        // Then no hit and no matched entity
        assertThat(report.sanctionsHit()).isFalse();
        assertThat(report.matchedEntity()).isNull();
    }
}

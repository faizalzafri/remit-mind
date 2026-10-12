package com.remitmind.ai.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Audit record of one parse() call: the extracted transfer and the decision on it.
 */
@Entity
public class TransferDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    private Instant createdAt;

    private String senderName;
    private String receiverName;
    private BigDecimal sourceAmount;
    private String sourceCurrency;
    private String targetCurrency;
    private String destinationCountry;
    private String purpose;
    private String status;
    private String riskLevel;
    private String rationale;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> requiredDocuments;

    private boolean sanctionsHit;
    private String matchedEntity;

    protected TransferDecision() {
        // for JPA
    }

    public TransferDecision(Transaction transaction, RiskAuditReport report) {
        this.senderName = transaction.senderName();
        this.receiverName = transaction.receiverName();
        this.sourceAmount = transaction.sourceAmount() == null ? null : BigDecimal.valueOf(transaction.sourceAmount());
        this.sourceCurrency = transaction.sourceCurrency();
        this.targetCurrency = transaction.targetCurrency();
        this.destinationCountry = transaction.destinationCountry();
        this.purpose = transaction.purpose();
        this.status = report.status();
        this.riskLevel = report.riskLevel();
        this.rationale = report.rationale();
        this.requiredDocuments = report.requiredDocuments();
        this.sanctionsHit = report.sanctionsHit();
        this.matchedEntity = report.matchedEntity();
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public List<String> getRequiredDocuments() { return requiredDocuments; }
}

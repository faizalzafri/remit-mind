package com.remitmind.ai.domain;

/**
 * Immutable record representing the extracted remittance transaction details.
 */
public record Transaction(
    String senderName,
    String receiverName,
    Double sourceAmount,
    String sourceCurrency,
    String targetCurrency,
    String destinationCountry,
    String purpose
) {}

package com.remitmind.ai.domain;

/**
 * Transfer details extracted from a user's message.
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

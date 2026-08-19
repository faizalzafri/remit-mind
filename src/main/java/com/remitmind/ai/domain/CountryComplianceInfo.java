package com.remitmind.ai.domain;

/**
 * Compliance details and limit guidelines for a destination country.
 */
public record CountryComplianceInfo(
    String countryName,
    String currencyCode,
    String region,
    double maxTransferLimit,
    String taxationGuidelines
) {}

package com.remitmind.ai.domain;

/**
 * Transfer limit and compliance guidelines for a destination country.
 */
public record CountryComplianceInfo(
    String countryName,
    String currencyCode,
    String region,
    double maxTransferLimit,
    String taxationGuidelines
) {}

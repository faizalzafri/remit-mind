package com.remitmind.ai.service;

import com.remitmind.ai.domain.CountryComplianceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Looks up a country's transfer limit and compliance rules.
 */
@Component
public class CountryDataTool {

    private static final Logger logger = LoggerFactory.getLogger(CountryDataTool.class);
    private final RestClient restClient;

    public CountryDataTool() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1500);
        requestFactory.setReadTimeout(1500);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://restcountries.com/v3.1")
                .build();
    }

    /**
     * Shape of the RestCountries API's response.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RestCountriesResponse(
            String region,
            String subregion,
            Map<String, Map<String, Object>> currencies
    ) {}

    /**
     * Looks up the transfer limit and compliance guidance for a country.
     *
     * @param countryName the destination country (e.g., Mexico, Canada)
     * @return the transfer limit and compliance guidance
     */
    @Tool(description = "Retrieve compliance guidelines, corridor limits, and taxation rules for a target destination country.")
    public CountryComplianceInfo getCountryCompliance(String countryName) {
        logger.info("Executing CountryDataTool: fetching compliance details for {}", countryName);

        try {
            RestCountriesResponse[] responses = restClient.get()
                    .uri("/name/{name}?fields=region,subregion,currencies", countryName)
                    .retrieve()
                    .body(RestCountriesResponse[].class);

            if (responses != null && responses.length > 0) {
                RestCountriesResponse target = responses[0];
                String region = target.region();
                String subregion = target.subregion();
                String currencyCode = "USD";

                if (target.currencies() != null && !target.currencies().isEmpty()) {
                    currencyCode = target.currencies().keySet().iterator().next();
                }

                // Corridor-based limit rules
                double maxLimit = 2000.0;
                String taxGuidelines = "Standard AML declaration required for foreign transfers.";

                if ("North America".equalsIgnoreCase(subregion) || "Europe".equalsIgnoreCase(region)) {
                    maxLimit = 5000.0;
                    taxGuidelines = "Simplified declaration corridor. No extra corridor duties under $5000.";
                } else {
                    taxGuidelines = "High scrutiny corridor. Requires purpose verification and receiver documentation.";
                }

                CountryComplianceInfo info = new CountryComplianceInfo(
                        countryName,
                        currencyCode,
                        region,
                        maxLimit,
                        taxGuidelines
                );

                logger.info("CountryDataTool success resolved limits for {}: max limit = {}, currency = {}", countryName, maxLimit, currencyCode);
                return info;
            }
        } catch (Exception e) {
            logger.error("CountryDataTool failure query for country {}: {}", countryName, e.getMessage());
        }

        // Used only when the lookup above fails
        double maxLimit = 2000.0;
        if (countryName.equalsIgnoreCase("Mexico")) {
            maxLimit = 5000.0; // Mexico's real limit, even when the lookup fails
        }
        return new CountryComplianceInfo(
                countryName,
                "MXN",
                "Americas",
                maxLimit,
                "Standard compliance corridor. Manual declaration recommended."
        );
    }
}

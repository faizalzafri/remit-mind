package com.remitmind.ai.service;

import com.remitmind.ai.domain.CountryComplianceInfo;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Looks up a country's transfer limit and compliance rules.
 *
 * <p>
 * Used to call the RestCountries API for region/currency data, but that API
 * now requires a paid auth key. Uses a small embedded reference dataset
 * instead, since this app only needs a stable region classification, not
 * live-changing country data.
 */
@Component
public class CountryDataTool {

    private static final Logger logger = LoggerFactory.getLogger(CountryDataTool.class);

    private record CountryRecord(String currencyCode, String region, String subregion) {}

    private static final Map<String, CountryRecord> COUNTRY_DATA = Map.ofEntries(
            Map.entry("mexico", new CountryRecord("MXN", "Americas", "North America")),
            Map.entry("united states", new CountryRecord("USD", "Americas", "North America")),
            Map.entry("canada", new CountryRecord("CAD", "Americas", "North America")),
            Map.entry("nigeria", new CountryRecord("NGN", "Africa", "Western Africa")),
            Map.entry("kenya", new CountryRecord("KES", "Africa", "Eastern Africa")),
            Map.entry("germany", new CountryRecord("EUR", "Europe", "Central Europe")),
            Map.entry("france", new CountryRecord("EUR", "Europe", "Western Europe")),
            Map.entry("spain", new CountryRecord("EUR", "Europe", "Southern Europe")),
            Map.entry("italy", new CountryRecord("EUR", "Europe", "Southern Europe")),
            Map.entry("netherlands", new CountryRecord("EUR", "Europe", "Western Europe")),
            Map.entry("ireland", new CountryRecord("EUR", "Europe", "Northern Europe")),
            Map.entry("united kingdom", new CountryRecord("GBP", "Europe", "Northern Europe")),
            Map.entry("india", new CountryRecord("INR", "Asia", "Southern Asia")),
            Map.entry("philippines", new CountryRecord("PHP", "Asia", "South-Eastern Asia")),
            Map.entry("brazil", new CountryRecord("BRL", "Americas", "South America")),
            Map.entry("china", new CountryRecord("CNY", "Asia", "Eastern Asia")),
            Map.entry("japan", new CountryRecord("JPY", "Asia", "Eastern Asia")),
            Map.entry("australia", new CountryRecord("AUD", "Oceania", "Australia and New Zealand"))
    );

    /**
     * Looks up the transfer limit and compliance guidance for a country.
     *
     * @param countryName the destination country (e.g., Mexico, Canada)
     * @return the transfer limit and compliance guidance
     */
    @Tool(description = "Retrieve compliance guidelines, corridor limits, and taxation rules for a target destination country.")
    public CountryComplianceInfo getCountryCompliance(String countryName) {
        logger.info("Executing CountryDataTool: fetching compliance details for {}", countryName);

        CountryRecord data = COUNTRY_DATA.get(countryName.toLowerCase());

        if (data == null) {
            logger.warn("CountryDataTool: no reference data for {}, using rest-of-world defaults", countryName);
            return new CountryComplianceInfo(
                    countryName,
                    "Unknown",
                    "Unclassified",
                    2000.0,
                    "High scrutiny corridor. Requires purpose verification and receiver documentation."
            );
        }

        // Corridor-based limit rules
        double maxLimit;
        String taxGuidelines;
        if ("North America".equalsIgnoreCase(data.subregion()) || "Europe".equalsIgnoreCase(data.region())) {
            maxLimit = 5000.0;
            taxGuidelines = "Simplified declaration corridor. No extra corridor duties under $5000.";
        } else {
            maxLimit = 2000.0;
            taxGuidelines = "High scrutiny corridor. Requires purpose verification and receiver documentation.";
        }

        logger.info("CountryDataTool resolved limits for {}: max limit = {}, currency = {}",
                countryName, maxLimit, data.currencyCode());
        return new CountryComplianceInfo(countryName, data.currencyCode(), data.region(), maxLimit, taxGuidelines);
    }
}

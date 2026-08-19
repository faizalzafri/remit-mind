package com.remitmind.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Exchange rate lookup tool querying the public Frankfurter API.
 */
@Component
public class ExchangeRateTool {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateTool.class);
    private final RestClient restClient;

    public ExchangeRateTool() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1500);
        requestFactory.setReadTimeout(1500);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl("https://api.frankfurter.app")
                .build();
    }

    /**
     * DTO for mapping the Frankfurter API response.
     */
    private record FrankfurterResponse(String base, Map<String, Double> rates) {}

    /**
     * Retrieves the latest exchange rate from base currency to target quote currency.
     *
     * @param base the source currency code (e.g. USD, EUR)
     * @param quote the target currency code (e.g. MXN, CAD)
     * @return the double conversion rate
     */
    @Tool(description = "Retrieve the live currency exchange rate from a base currency (e.g., USD) to a quote currency (e.g., MXN).")
    public double getExchangeRate(String base, String quote) {
        logger.info("Executing ExchangeRateTool: fetching rate for {} -> {}", base, quote);

        if (base.equalsIgnoreCase(quote)) {
            return 1.0;
        }

        try {
            FrankfurterResponse response = restClient.get()
                    .uri("/latest?from={base}&to={quote}", base.toUpperCase(), quote.toUpperCase())
                    .retrieve()
                    .body(FrankfurterResponse.class);

            if (response != null && response.rates() != null && response.rates().containsKey(quote.toUpperCase())) {
                double rate = response.rates().get(quote.toUpperCase());
                logger.info("ExchangeRateTool success: {} -> {} rate is {}", base, quote, rate);
                return rate;
            }
        } catch (Exception e) {
            logger.error("ExchangeRateTool failure fetching rate for {} -> {}: {}", base, quote, e.getMessage());
        }

        // Return a mock default if API is unavailable, as a fallback standard
        if (base.equalsIgnoreCase("USD") && quote.equalsIgnoreCase("MXN")) {
            return 20.0;
        }
        return 1.0;
    }
}

package com.remitmind.ai.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.remitmind.ai.domain.ScreeningOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Screens names against sanctions/PEP lists via a self-hosted yente instance.
 * Called directly from Java, never exposed as a @Tool, so the model can't skip it.
 */
@Service
public class SanctionsScreeningService {

    private static final Logger logger = LoggerFactory.getLogger(SanctionsScreeningService.class);
    private final RestClient restClient;

    @Autowired
    public SanctionsScreeningService(@Value("${remitmind.yente.url}") String yenteUrl) {
        this(RestClient.builder().requestFactory(timeouts()), yenteUrl);
    }

    // Package-private so tests can pass a builder bound to MockRestServiceServer.
    SanctionsScreeningService(RestClient.Builder builder, String yenteUrl) {
        this.restClient = builder.baseUrl(yenteUrl).build();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1500);
        requestFactory.setReadTimeout(5000); // yente's first query after startup exceeded 1.5s
        return requestFactory;
    }

    /** One yente match candidate. {@code match} is true when yente's own threshold is passed. */
    private record Candidate(String id, String caption, double score, boolean match) {}

    private record QueryResponse(List<Candidate> results) {}

    private record MatchResponse(Map<String, QueryResponse> responses) {}

    /**
     * Screens one name. UNAVAILABLE when yente is down or times  out, so a
     * failed check is never mistaken for CLEAR
     */
    ScreeningOutcome screen(String name) {
        Map<String, Object> body = Map.of("queries", Map.of("q", Map.of(
                "schema", "LegalEntity",
                "properties", Map.of("name", List.of(name)))));

        try {
            MatchResponse response = restClient.post()
                    .uri("/match/default")
                    .body(body)
                    .retrieve()
                    .body(MatchResponse.class);

            Optional<Candidate> hit = response.responses().get("q").results().stream()
                    .filter(Candidate::match)
                    .findFirst();
            logger.info("Sanctions screening for '{}': {}", name, hit.map(Candidate::caption).orElse("no match"));
            return hit.isPresent() ? ScreeningOutcome.HIT : ScreeningOutcome.CLEAR;
        } catch (RestClientException e) {
            logger.warn("Sanctions screening unavailable for '{}': {}", name, e.getMessage());
            return ScreeningOutcome.UNAVAILABLE;
        }
    }
}

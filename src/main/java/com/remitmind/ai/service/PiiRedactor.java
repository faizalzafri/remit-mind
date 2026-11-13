package com.remitmind.ai.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * Redacts personal data from free text before it's logged, using a self-hosted
 * Presidio analyzer. Never used on the sanctions screening path: yente needs real names.
 */
@Service
public class PiiRedactor {

    static final String PLACEHOLDER = "[REDACTED]";

    private final RestClient restClient;

    @Autowired
    public PiiRedactor(@Value("${remitmind.presidio.url}") String presidioUrl) {
        this(RestClient.builder().requestFactory(timeouts()), presidioUrl);
    }

    // Package-private so tests can pass a builder bound to MockRestServiceServer.
    PiiRedactor(RestClient.Builder builder, String presidioUrl) {
        this.restClient = builder.baseUrl(presidioUrl).build();
    }

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1500);
        requestFactory.setReadTimeout(1500);
        return requestFactory;
    }

    /**
     * One PII span Presidio found: characters [start, end) are of this type.
     */
    private record Entity(@JsonProperty("entity_type") String type, int start, int end) {
    }

    /**
     * Returns the text with each PII span replaced by its type, e.g. "Send <PERSON> 100 USD".
     * Returns a fixed placeholder if Presidio fails, so raw text never reaches a log.
     */
    public String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        try {
            Entity[] found = restClient.post()
                    .uri("/analyze")
                    .body(Map.of("text", text, "language", "en"))
                    .retrieve()
                    .body(Entity[].class);

            // Walk spans left to right; skipping or extending past overlaps keeps every span covered.
            StringBuilder out = new StringBuilder();
            int pos = 0;
            for (Entity e : Arrays.stream(found).sorted(Comparator.comparingInt(Entity::start)).toList()) {
                if (e.end() <= pos) {
                    continue;
                }
                out.append(text, pos, Math.max(pos, e.start())).append('<').append(e.type()).append('>');
                pos = e.end();
            }
            return out.append(text.substring(pos)).toString();
        } catch (RuntimeException e) {
            return PLACEHOLDER; // fail closed for privacy: Presidio down, timed out, or bad offsets
        }
    }
}

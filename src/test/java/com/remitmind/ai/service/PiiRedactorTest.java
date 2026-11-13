package com.remitmind.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PiiRedactorTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final PiiRedactor redactor = new PiiRedactor(builder, "http://presidio");

    @Test
    void replacesSpansPresidioFinds() {
        // Given Presidio finds a person at characters 5-16
        server.expect(requestTo("http://presidio/analyze"))
                .andRespond(withSuccess("""
                        [{"entity_type": "PERSON", "start": 5, "end": 16, "score": 0.85}]
                        """, MediaType.APPLICATION_JSON));

        // When redacted
        // Then the name is replaced by its type
        assertThat(redactor.redact("Send Maria Lopez 100 USD")).isEqualTo("Send <PERSON> 100 USD");
    }

    @Test
    void overlappingSpansLeaveNothingUncovered() {
        // Given two overlapping finds covering characters 0-16
        server.expect(requestTo("http://presidio/analyze"))
                .andRespond(withSuccess("""
                        [{"entity_type": "PERSON", "start": 5, "end": 16, "score": 0.85},
                         {"entity_type": "LOCATION", "start": 0, "end": 10, "score": 0.40}]
                        """, MediaType.APPLICATION_JSON));

        // When redacted
        // Then no part of either span survives
        assertThat(redactor.redact("Send Maria Lopez 100 USD")).isEqualTo("<LOCATION><PERSON> 100 USD");
    }

    @Test
    void presidioDownGivesPlaceholderNotRawText() {
        // Given Presidio fails
        server.expect(requestTo("http://presidio/analyze")).andRespond(withServerError());

        // When redacted
        // Then the raw text never comes back
        assertThat(redactor.redact("Send Maria Lopez 100 USD")).isEqualTo(PiiRedactor.PLACEHOLDER);
    }
}

package com.remitmind.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.remitmind.ai.domain.ScreeningOutcome;
import com.remitmind.ai.domain.ScreeningResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;

class SanctionsScreeningServiceTest {

    private static final String YENTE_REPLY = """
            {"responses": {"q": {"results": [
              {"id": "a", "caption": "Viktor Bout", "score": 0.95, "match": %s},
              {"id": "b", "caption": "Someone Else", "score": 0.40, "match": false}
            ]}}}
            """;

    @Test
    void returnsTopCandidateWhenYenteFlagsAMatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SanctionsScreeningService service = new SanctionsScreeningService(builder, "http://yente");

        server.expect(requestTo("http://yente/match/default"))
                .andExpect(jsonPath("$.queries.q.properties.name[0]").value("Viktor Bout"))
                .andRespond(withSuccess(YENTE_REPLY.formatted("true"), MediaType.APPLICATION_JSON));

        assertThat(service.screen("Viktor Bout")).isEqualTo(new ScreeningResult(ScreeningOutcome.HIT, "Viktor Bout"));
        server.verify();
    }

    @Test
    void returnsClearWhenNoCandidateIsAMatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SanctionsScreeningService service = new SanctionsScreeningService(builder, "http://yente");

        server.expect(requestTo("http://yente/match/default"))
                .andRespond(withSuccess(YENTE_REPLY.formatted("false"), MediaType.APPLICATION_JSON));

        assertThat(service.screen("Maria Lopez")).isEqualTo(ScreeningResult.CLEAR);
    }

    @Test
    void returnsUnavailableWhenYenteReturnsServerError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SanctionsScreeningService service = new SanctionsScreeningService(builder, "http://yente");

        server.expect(requestTo("http://yente/match/default")).andRespond(withServerError());

        assertThat(service.screen("Viktor Bout")).isEqualTo(new ScreeningResult(ScreeningOutcome.UNAVAILABLE, null));
    }

    @Test
    void returnsUnavailableWhenYenteTimesOut() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SanctionsScreeningService service = new SanctionsScreeningService(builder, "http://yente");

        server.expect(requestTo("http://yente/match/default"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThat(service.screen("Viktor Bout")).isEqualTo(new ScreeningResult(ScreeningOutcome.UNAVAILABLE, null));
    }

}

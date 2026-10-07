package com.remitmind.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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

        assertThat(service.screen("Viktor Bout"))
                .hasValueSatisfying(c -> assertThat(c.caption()).isEqualTo("Viktor Bout"));
        server.verify();
    }

    @Test
    void returnsEmptyWhenNoCandidateIsAMatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        SanctionsScreeningService service = new SanctionsScreeningService(builder, "http://yente");

        server.expect(requestTo("http://yente/match/default"))
                .andRespond(withSuccess(YENTE_REPLY.formatted("false"), MediaType.APPLICATION_JSON));

        assertThat(service.screen("Maria Lopez")).isEmpty();
    }
}

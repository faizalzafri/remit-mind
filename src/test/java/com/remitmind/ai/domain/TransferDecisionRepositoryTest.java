package com.remitmind.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TransferDecisionRepositoryTest {

    @TestConfiguration
    static class Postgres {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }

    @Autowired
    TransferDecisionRepository repository;

    @Autowired
    TestEntityManager entityManager;

    @Test
    void savesDecisionAndReadsItBack() {
        // Given a flagged transfer with required documents
        Transaction transaction = new Transaction("Maria Lopez", "Juan Perez", 6000.0, "USD", "MXN", "Mexico", "family support");
        RiskAuditReport report = new RiskAuditReport("FLAG_MANUAL_REVIEW", "MEDIUM", "Over corridor limit",
                List.of("Proof of Funds", "ID Card"), false, null);

        // When saved and read back from the database, not the cache
        Long id = repository.save(new TransferDecision(transaction, report)).getId();
        entityManager.flush();
        entityManager.clear();
        TransferDecision saved = repository.findById(id).orElseThrow();

        // Then the decision survived the round trip
        assertThat(saved.getStatus()).isEqualTo("FLAG_MANUAL_REVIEW");
        assertThat(saved.getRequiredDocuments()).containsExactly("Proof of Funds", "ID Card");
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}

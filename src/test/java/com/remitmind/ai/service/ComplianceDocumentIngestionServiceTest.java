package com.remitmind.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

class ComplianceDocumentIngestionServiceTest {

    @Test
    void splitsComplianceRulesIntoMultipleChunksWithMetadata() {
        List<Document> chunks = ComplianceDocumentIngestionService
                .loadAndSplit(new ClassPathResource("documents/compliance-rules.txt"));

        assertThat(chunks).hasSizeGreaterThan(1);

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            assertThat(chunk.getText()).isNotBlank();
            assertThat(chunk.getMetadata())
                    .containsEntry("chunk_index", i)
                    .containsEntry("total_chunks", chunks.size())
                    .containsEntry("category", "compliance-rules")
                    .containsKey("source");
        }
    }
}

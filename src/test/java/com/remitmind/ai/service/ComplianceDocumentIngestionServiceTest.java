package com.remitmind.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

class ComplianceDocumentIngestionServiceTest {

    @Test
    void splitsComplianceRulesIntoMultipleChunksWithMetadata() {
        // Given the compliance rulebook file

        // When it is loaded and split into chunks
        List<Document> chunks = ComplianceDocumentIngestionService
                .loadAndSplit(new ClassPathResource("documents/compliance-rules.txt"));

        // Then it produces more than one chunk, each with text and tracking metadata
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

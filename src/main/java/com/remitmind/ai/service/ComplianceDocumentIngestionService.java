package com.remitmind.ai.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Loads the compliance rulebook at startup, splits it into chunks, and stores
 * them in the vector store so they can be searched later. VectorStore.add(...)
 * embeds each chunk automatically.
 */
@Component
public class ComplianceDocumentIngestionService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceDocumentIngestionService.class);

    // Smaller than TokenTextSplitter's own default so this small rulebook still
    // splits into several chunks.
    static final int CHUNK_SIZE_TOKENS = 100;
    static final int MIN_CHUNK_SIZE_CHARS = 100;

    @Value("classpath:documents/compliance-rules.txt")
    private Resource complianceRulesResource;

    private final VectorStore vectorStore;

    public ComplianceDocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        List<Document> chunks = loadAndSplit(complianceRulesResource);

        logger.info("Compliance ingestion: {} chunk(s) produced from {}", chunks.size(),
                complianceRulesResource.getFilename());

        vectorStore.add(chunks);

        logger.info("Compliance ingestion: {} chunk(s) added to the vector store", chunks.size());
    }

    /**
     * Reads the file and splits it into chunks. Package-private so tests can call
     * it directly without starting the full application.
     */
    static List<Document> loadAndSplit(Resource resource) {
        TextReader reader = new TextReader(resource);
        reader.getCustomMetadata().put("category", "compliance-rules");

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(CHUNK_SIZE_TOKENS)
                .withMinChunkSizeChars(MIN_CHUNK_SIZE_CHARS)
                .build();

        return splitter.split(reader.get());
    }
}

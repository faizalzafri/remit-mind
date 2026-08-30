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
 * Loads the compliance rulebook, splits it into retrievable chunks, and persists
 * the chunks into the vector store.
 *
 * <p>
 * {@code VectorStore.add(...)} embeds each chunk internally via the configured
 * EmbeddingModel, so no manual embedding step is needed here.
 */
@Component
public class ComplianceDocumentIngestionService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ComplianceDocumentIngestionService.class);

    // Deliberately small compared to TokenTextSplitter's own default (800 tokens)
    // so a handful of compliance paragraphs actually produce multiple chunks to
    // inspect.
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
     * Reads the given resource and splits it into token-bounded chunks.
     * Package-private so the chunking behavior can be exercised directly in tests
     * without needing a full Spring context or a Gemini API key.
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

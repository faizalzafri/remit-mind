package com.remitmind.ai.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Loads the compliance rulebook, splits it into retrievable chunks, and embeds
 * each
 * chunk.
 *
 * <p>
 * This only proves out reading + chunking + embedding. No vector store yet —
 * chunks
 * and their vectors are logged, not persisted anywhere.
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

    private final EmbeddingModel embeddingModel;

    public ComplianceDocumentIngestionService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public void run(String... args) {
        List<Document> chunks = loadAndSplit(complianceRulesResource);

        logger.info("Compliance ingestion: {} chunk(s) produced from {}", chunks.size(),
                complianceRulesResource.getFilename());

        List<float[]> vectors = chunks.stream().map(chunk -> embeddingModel.embed(chunk.getText())).toList();

        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            String text = chunk.getText();
            String preview = text.length() > 120 ? text.substring(0, 120) + "..." : text;
            logger.info("Chunk {}/{} [{} chars, {} dims] metadata={} :: {}",
                    chunk.getMetadata().get("chunk_index"), chunk.getMetadata().get("total_chunks"),
                    text.length(), vectors.get(i).length, chunk.getMetadata(), preview);
        }

        // Recap: For the Nigeria rule (chunk 1) and its NGO exception (chunk 2)
        // ended up lexically disjoint after splitting.

        // This checks whether embeddings recover that relationship semantically despite
        // the split.
        if (vectors.size() > 2) {
            double similarity = cosineSimilarity(vectors.get(1), vectors.get(2));
            logger.info("Cosine similarity between chunk 1 (Nigeria rule) and chunk 2 (NGO exception): {}",
                    similarity);
        }
    }

    /**
     * Cosine similarity between two equal-length vectors, in [-1, 1] (in practice
     * embedding vectors are non-negative-dominant, so typically [0, 1]).
     * Package-private so it can be tested directly without a model call.
     */
    static double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
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

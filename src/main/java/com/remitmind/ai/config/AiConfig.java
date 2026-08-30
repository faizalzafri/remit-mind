package com.remitmind.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * AI configuration for the RemitMind copilot.
 */
@Configuration
public class AiConfig {

    public static final double SIMILARITY_THRESHOLD = 0.5;
    public static final int TOP_K = 3;

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(systemPromptResource)
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * RAG advisor over the compliance rulebook. Uses a custom prompt template that
     * appends retrieved context below the user's original message instead of the
     * default ContextualQueryAugmenter behavior, which replaces the message
     * entirely
     * with a "Context is below... Query: ... Answer:" template.
     *
     * allowEmptyContext(true) means messages with no matching compliance context
     * (most messages) pass through completely unchanged.
     *
     * Ordered to run after {@code PromptGuardrailAdvisor} (-100) so a blocked
     * prompt
     * never pays for retrieval, and inside {@code RequestTraceIdAdvisor}'s (0)
     * timing
     * so its latency measurement includes retrieval.
     */
    @Bean
    Advisor complianceRetrievalAdvisor(VectorStore vectorStore) {
        PromptTemplate contextTemplate = new PromptTemplate("""
                {query}
                
                ---
                Relevant compliance context (base your compliance determination on this,
                including any exceptions; if it does not address the situation, say so
                rather than guessing):
                {context}
                ---
                """);

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .topK(TOP_K)
                .build();

        var queryAugmenter = ContextualQueryAugmenter.builder()
                .promptTemplate(contextTemplate)
                .allowEmptyContext(true)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(queryAugmenter)
                .order(50)
                .build();
    }
}

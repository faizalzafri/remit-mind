package com.remitmind.ai.service;

import com.remitmind.ai.config.PromptGuardrailAdvisor;
import com.remitmind.ai.config.RequestTraceIdAdvisor;
import com.remitmind.ai.domain.CopilotResponse;
import java.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Core copilot service that orchestrates AI interactions.
 *
 * <p>
 * Demonstrates clean separation: business logic is focused here,
 * while security scanning, tracing, and timing execution are handled
 * by decoupled standalone advisors.
 *
 * <p>
 * Conversation history memory is managed by the MessageChatMemoryAdvisor.
 */
@Service
public class RemittanceCopilotService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public RemittanceCopilotService(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * Sends a user message to the LLM and returns the response as plain text.
     * Manages multi-turn conversation memory using the session ID.
     * 
     * @param sessionId   the session identifier for conversation history tracking
     * @param userMessage the natural language input from the user
     * @return the model's text response
     */
    public String chat(String sessionId, String userMessage) {
        return chatClient.prompt()
                .advisors(new PromptGuardrailAdvisor(), new RequestTraceIdAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .system(s -> s.param("currentDate", LocalDate.now().toString()))
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Sends a user message to the LLM and returns a structured, parsed response.
     * Maps the natural language text directly into the CopilotResponse record.
     *
     * @param userMessage the natural language request containing transfer intents
     * @return the parsed CopilotResponse object
     */
    public CopilotResponse parse(String userMessage) {
        return chatClient.prompt()
                .advisors(new PromptGuardrailAdvisor(), new RequestTraceIdAdvisor())
                .system(s -> s.param("currentDate", LocalDate.now().toString()))
                .user(userMessage)
                .call()
                .entity(CopilotResponse.class);
    }
}

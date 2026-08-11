package com.remitmind.ai.service;

import com.remitmind.ai.domain.CopilotResponse;
import java.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * Core copilot service that orchestrates AI interactions.
 *
 * At this stage, the service is stateless: each call is an independent
 * LLM request with no memory of previous interactions. The system prompt
 * (set in AiConfig) is sent with every request automatically.
 */
@Service
public class RemittanceCopilotService {

    private final ChatClient chatClient;

    public RemittanceCopilotService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * Sends a user message to the LLM and returns the response as plain text.
     * 
     * @param userMessage the natural language input from the user
     * @return the model's text response
     */
    public String chat(String userMessage) {
        return chatClient.prompt()
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
                .system(s -> s.param("currentDate", LocalDate.now().toString()))
                .user(userMessage)
                .call()
                .entity(CopilotResponse.class);
    }
}

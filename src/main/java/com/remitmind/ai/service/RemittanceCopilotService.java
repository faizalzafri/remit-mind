package com.remitmind.ai.service;

import com.remitmind.ai.config.PromptGuardrailAdvisor;
import com.remitmind.ai.config.RequestTraceIdAdvisor;
import com.remitmind.ai.domain.CopilotResponse;
import java.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Sends user messages to the model. Security checks, timing, and compliance
 * lookups are handled by advisors attached to each call, not here.
 */
@Service
public class RemittanceCopilotService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ExchangeRateTool exchangeRateTool;
    private final CountryDataTool countryDataTool;
    private final Advisor complianceRetrievalAdvisor;

    public RemittanceCopilotService(ChatClient chatClient, ChatMemory chatMemory,
                                    ExchangeRateTool exchangeRateTool, CountryDataTool countryDataTool,
                                    Advisor complianceRetrievalAdvisor) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.exchangeRateTool = exchangeRateTool;
        this.countryDataTool = countryDataTool;
        this.complianceRetrievalAdvisor = complianceRetrievalAdvisor;
    }

    /**
     * Sends a message and returns a plain-text reply. Remembers earlier messages
     * in the same session.
     *
     * @param sessionId   identifies the conversation to remember
     * @param userMessage the user's message
     * @return the model's text reply
     */
    public String chat(String sessionId, String userMessage) {
        return chatClient.prompt()
                .advisors(new PromptGuardrailAdvisor(), new RequestTraceIdAdvisor(), complianceRetrievalAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .tools(exchangeRateTool, countryDataTool)
                .system(s -> s.param("currentDate", LocalDate.now().toString()))
                .user(userMessage)
                .call()
                .content();
    }

    /**
     * Sends a message and returns the extracted transfer plus its compliance
     * check. Does not remember earlier messages.
     *
     * @param userMessage the user's transfer request
     * @return the extracted transfer and its compliance check
     */
    public CopilotResponse parse(String userMessage) {
        return chatClient.prompt()
                .advisors(new PromptGuardrailAdvisor(), new RequestTraceIdAdvisor(), complianceRetrievalAdvisor)
                .tools(exchangeRateTool, countryDataTool)
                .system(s -> s.param("currentDate", LocalDate.now().toString()))
                .user(userMessage)
                .call()
                .entity(CopilotResponse.class);
    }
}

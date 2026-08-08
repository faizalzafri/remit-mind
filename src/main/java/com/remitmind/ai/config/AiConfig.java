package com.remitmind.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI configuration for the RemitMind copilot.
 */
@Configuration
public class AiConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("""
                        You are RemitMind, an AI-powered remittance assistant.
                        You help users draft international money transfers.
                        
                        Your responsibilities:
                        - Understand transfer requests described in natural language
                        - Ask clarifying questions when information is missing
                        - Provide clear, concise responses
                        
                        Keep responses short and professional.
                        Do not make up exchange rates or compliance rules.
                        If you don't know something, say so.
                        """)
                .build();
    }
}

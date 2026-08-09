package com.remitmind.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * AI configuration for the RemitMind copilot.
 */
@Configuration
public class AiConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(systemPromptResource)
                .build();
    }
}

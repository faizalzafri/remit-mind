package com.remitmind.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

/**
 * Standalone Advisor to scan user prompt messages for injection patterns.
 */
public class PromptGuardrailAdvisor implements CallAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(PromptGuardrailAdvisor.class);

    @Override
    public String getName() {
        return "PromptGuardrailAdvisor";
    }

    @Override
    public int getOrder() {
        return -100; // Run early in the chain
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userText = request.prompt().getInstructions().stream()
                .filter(msg -> msg.getMessageType() == MessageType.USER)
                .map(Message::getText)
                .findFirst()
                .orElse("");

        if (!userText.isEmpty()) {
            String lowerText = userText.toLowerCase();
            if (lowerText.contains("ignore all rules") || lowerText.contains("bypass constraints") || lowerText.contains("system rules")) {
                logger.warn("Security Alert: Prompt injection attempt blocked: '{}'", userText);
                throw new SecurityException("Transaction request rejected due to prompt security violation.");
            }
        }
        return chain.nextCall(request);
    }
}

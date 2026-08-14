package com.remitmind.ai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

import java.util.UUID;

/**
 * Standalone Advisor to inject execution trace ID and monitor response latency.
 */
public class RequestTraceIdAdvisor implements CallAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(RequestTraceIdAdvisor.class);

    @Override
    public String getName() {
        return "RequestTraceIdAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // Default execution order
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String auditTraceId = UUID.randomUUID().toString();
        request.context().put("auditTraceId", auditTraceId);
        logger.info("[Audit ID: {}] Starting remittance copilot request...", auditTraceId);

        long startTime = System.currentTimeMillis();
        ChatClientResponse response = chain.nextCall(request);
        long duration = System.currentTimeMillis() - startTime;

        logger.info("[Audit ID: {}] AI response returned in {} ms.", auditTraceId, duration);
        return response;
    }
}

package com.remitmind.ai.controller;

import com.remitmind.ai.domain.CopilotResponse;
import com.remitmind.ai.service.RemittanceCopilotService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the RemitMind copilot.
 */
@RestController
@RequestMapping("/api/copilot")
public class RemittanceController {

    private final RemittanceCopilotService copilotService;

    public RemittanceController(RemittanceCopilotService copilotService) {
        this.copilotService = copilotService;
    }

    /**
     * Simple chat endpoint: send a message, get a response.
     * No memory, no structured output — just text in, text out.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String response = copilotService.chat(request.message());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    /**
     * Parse endpoint: takes natural language request and extracts structured transaction data.
     */
    @PostMapping("/parse")
    public ResponseEntity<CopilotResponse> parse(@Valid @RequestBody ChatRequest request) {
        CopilotResponse response = copilotService.parse(request.message());
        return ResponseEntity.ok(response);
    }

    /**
     * Request DTO for the chat endpoint.
     */
    public record ChatRequest(
            @NotBlank(message = "Message must not be blank")
            String message
    ) {}

    /**
     * Response DTO for the chat endpoint.
     */
    public record ChatResponse(String response) {}
}

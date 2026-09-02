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
     * Send a message, get a plain-text reply. Remembers the conversation across
     * calls that share the same sessionId.
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : java.util.UUID.randomUUID().toString();
        String response = copilotService.chat(sessionId, request.message());
        return ResponseEntity.ok(new ChatResponse(response));
    }

    /**
     * Send a message, get back the extracted transfer and its compliance check
     * as structured data. Does not remember earlier messages.
     */
    @PostMapping("/parse")
    public ResponseEntity<CopilotResponse> parse(@Valid @RequestBody ChatRequest request) {
        CopilotResponse response = copilotService.parse(request.message());
        return ResponseEntity.ok(response);
    }

    /**
     * Request body for both endpoints.
     */
    public record ChatRequest(
            @NotBlank(message = "Message must not be blank")
            String message,
            String sessionId
    ) {}

    /**
     * Response DTO for the chat endpoint.
     */
    public record ChatResponse(String response) {}
}

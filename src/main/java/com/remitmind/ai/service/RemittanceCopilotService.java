package com.remitmind.ai.service;

import com.remitmind.ai.config.PromptGuardrailAdvisor;
import com.remitmind.ai.config.RequestTraceIdAdvisor;
import com.remitmind.ai.domain.CopilotReply;
import com.remitmind.ai.domain.CopilotResponse;
import com.remitmind.ai.domain.CountryComplianceInfo;
import com.remitmind.ai.domain.RiskAuditReport;
import com.remitmind.ai.domain.Transaction;
import java.time.LocalDate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final ToolCallbackProvider mcpClientToolCallbacks;

    public RemittanceCopilotService(ChatClient chatClient, ChatMemory chatMemory,
                                    ExchangeRateTool exchangeRateTool, CountryDataTool countryDataTool,
                                    Advisor complianceRetrievalAdvisor,
                                    @Qualifier("mcpToolCallbacks") ToolCallbackProvider mcpClientToolCallbacks) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.exchangeRateTool = exchangeRateTool;
        this.countryDataTool = countryDataTool;
        this.complianceRetrievalAdvisor = complianceRetrievalAdvisor;
        this.mcpClientToolCallbacks = mcpClientToolCallbacks;
    }

    /**
     * Sends a message and returns a plain-text reply. Remembers earlier messages
     * in the same session.
     *
     * <p>
     * Also gives the model access to whatever tools are exposed by the MCP
     * servers configured under spring.ai.mcp.client.* (e.g. the sandboxed
     * Filesystem server) - discovered at connection time, not written here.
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
                .tools(exchangeRateTool, countryDataTool, mcpClientToolCallbacks)
                .system(s -> s.param("currentDate", LocalDate.now().toString()))
                .user(userMessage)
                .call()
                .content();
    }

    // Prompt for parse()'s second call: model writes the rationale only,
    // Java already decided status/riskLevel. Plain Java string, not the
    // shared system-prompt.st, since chat() doesn't have these values.
    private static final String RATIONALE_PROMPT = """
            You are RemitMind, an AI-powered compliance-aware remittance copilot.

            A compliance decision has already been computed for this transfer:
            status=%s, riskLevel=%s, requiredDocuments=%s, based on a corridor
            limit of %.2f for %s.

            Do not change this decision or assert a different status. Your only
            job is to write a `rationale` explaining it clearly, referencing the
            limit, and a short conversational `chatResponse` for the user.

            If a "Relevant compliance context" section appears below, factor it
            into your rationale -- including any documented exceptions (e.g.
            verified NGOs, disaster relief) or stricter guidance that add nuance
            to the decision above. If that context asks for something stricter
            than the decision above (e.g. extra documentation above a lower
            threshold), say so plainly in the rationale instead of leaving it
            out -- the decision itself still stands, but the rationale must not
            contradict or omit what the retrieved context says. If no such
            context is present, or it does not address the situation, rely on
            the corridor limit alone.

            Today's date is %s.
            """;

    /**
     * Sends a message and returns the extracted transfer plus its compliance
     * check. Does not remember earlier messages.
     *
     * <p>
     * Java decides status/riskLevel (see {@link RiskAuditReport#evaluate}), not
     * the model. The model only extracts the transaction and writes the
     * rationale.
     *
     * @param userMessage the user's transfer request
     * @return the extracted transfer and its compliance check
     */
    public CopilotResponse parse(String userMessage) {
        Transaction transaction = chatClient.prompt()
                .advisors(new PromptGuardrailAdvisor(), new RequestTraceIdAdvisor())
                .system(s -> s.param("currentDate", LocalDate.now().toString()))
                .user(userMessage)
                .call()
                .entity(Transaction.class);

        CountryComplianceInfo compliance = countryDataTool.getCountryCompliance(transaction.destinationCountry());
        RiskAuditReport baseline = RiskAuditReport.evaluate(transaction.sourceAmount(), compliance);

        CopilotReply reply = chatClient.prompt()
                .advisors(new PromptGuardrailAdvisor(), new RequestTraceIdAdvisor(), complianceRetrievalAdvisor)
                .tools(exchangeRateTool)
                .system(RATIONALE_PROMPT.formatted(baseline.status(), baseline.riskLevel(),
                        baseline.requiredDocuments(), compliance.maxTransferLimit(),
                        transaction.destinationCountry(), LocalDate.now()))
                .user(userMessage)
                .call()
                .entity(CopilotReply.class);

        RiskAuditReport auditReport = new RiskAuditReport(
                baseline.status(), baseline.riskLevel(), reply.rationale(), baseline.requiredDocuments());
        return new CopilotResponse(reply.chatResponse(), transaction, auditReport);
    }
}

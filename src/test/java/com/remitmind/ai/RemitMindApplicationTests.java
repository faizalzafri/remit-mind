package com.remitmind.ai;

import com.remitmind.ai.domain.CopilotResponse;
import com.remitmind.ai.domain.Transaction;
import com.remitmind.ai.service.RemittanceCopilotService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RemitMindApplicationTests {

    @Autowired
    private RemittanceCopilotService copilotService;

    @Test
    void contextLoads() {
        assertThat(copilotService).isNotNull();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testChatWithRelativeDate() {
        // We verify that the currentDate template parameter is injected and understood by Gemini
        String response = copilotService.chat(
            "According to the system instructions, what is today's date? Respond ONLY in YYYY-MM-DD format."
        );
        
        String expectedDate = LocalDate.now().toString();
        assertThat(response).contains(expectedDate);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
    void testParseRemittanceIntent() {
        CopilotResponse response = copilotService.parse(
            "Draft a transfer of 150 USD from Alice to Bob in Mexico for family support."
        );

        assertThat(response).isNotNull();
        assertThat(response.chatResponse()).isNotBlank();
        
        Transaction tx = response.parsedTransaction();
        assertThat(tx).isNotNull();
        assertThat(tx.senderName()).isEqualTo("Alice");
        assertThat(tx.receiverName()).isEqualTo("Bob");
        assertThat(tx.sourceAmount()).isEqualTo(150.0);
        assertThat(tx.sourceCurrency()).isEqualTo("USD");
        assertThat(tx.destinationCountry()).isEqualTo("Mexico");
        assertThat(tx.purpose()).containsIgnoringCase("support");
    }
}

package com.remitmind.ai;

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
}

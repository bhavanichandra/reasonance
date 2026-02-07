package com.themuler.reasonance.service.llm;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.themuler.reasonance.core.ServicePrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "gemini")
@RequiredArgsConstructor
public class GeminiService implements LlmService {

    private final Client client;

    @Value("${gemini.model}")
    private String model;


    @Override
    public String generate(ServicePrompt prompt, Object... args) {
        String promptTemplate = prompt.getPrompt();
        log.debug("Generating Gemini response using Gemini. Prompt template length: {}", promptTemplate.length());
        String promptText = String.format(promptTemplate, args);
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(Schema.fromJson(prompt.getSchema()))
                    .build();
            GenerateContentResponse response = client.models.generateContent(model, promptText, config);
            String text = response.text();
            if (text == null) {
                log.error("Gemini output text is null");
                throw new RuntimeException("Gemini output text is null");
            }
            log.debug("Gemini response generated successfully. Length: {}", text.length());
            return text;
        } catch (Exception e) {
            log.error("Error during Gemini generation", e);
            throw e;
        }
    }
}

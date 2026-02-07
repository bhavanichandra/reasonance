package com.themuler.reasonance.service.llm;

import com.themuler.reasonance.core.ServicePrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "ollama", matchIfMissing = true)
@RequiredArgsConstructor
public class OllamaService implements LlmService {

    private final OllamaChatModel chatModel;


    @Override
    public String generate(ServicePrompt prompt, Object... args) {
        String promptTemplate = prompt.getPrompt();
        log.debug("Generating LLM response using Ollama. Prompt template length: {}", promptTemplate.length());
        String promptText = String.format(promptTemplate, args);
        try {
            // Build options with schema
            OllamaChatOptions chatOptions = OllamaChatOptions.builder().build();

            if (prompt.getSchema() != null) {
                chatOptions.setOutputSchema(prompt.getSchema());
            }

            // Pass options to the call
            Prompt promptWithOptions = new Prompt(promptText, chatOptions);
            ChatResponse response = chatModel.call(promptWithOptions);

            Generation result = response.getResult();
            if (result == null) {
                log.error("LLM generation result is null");
                throw new RuntimeException("LLM generation result is null");
            }

            String text = result.getOutput().getText();
            if (text == null) {
                log.error("LLM output text is null");
                throw new RuntimeException("LLM output text is null");
            }

            log.debug("LLM response generated successfully. Length: {}", text.length());
            return text;
        } catch (Exception e) {
            log.error("Error during LLM generation", e);
            throw e;
        }
    }
}

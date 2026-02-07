package com.themuler.reasonance.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themuler.reasonance.dto.ParsedAnalyzeResult;
import com.themuler.reasonance.dto.ParsedFinalizeResult;
import com.themuler.reasonance.dto.ParsedInitializeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmResponseParser {

    private final ObjectMapper objectMapper;

    public ParsedInitializeResult parseInitialize(JsonNode rootNode) {
        return objectMapper.convertValue(rootNode, ParsedInitializeResult.class);
    }

    public ParsedAnalyzeResult parseAnalyze(JsonNode rootNode) {
        return objectMapper.convertValue(rootNode, ParsedAnalyzeResult.class);
    }

    public ParsedFinalizeResult parseFinalize(JsonNode rootNode) {
        if (rootNode.has("final_conclusion")) {
            return objectMapper.convertValue(rootNode, ParsedFinalizeResult.class);
        } else if (rootNode.has("final_conclusion_content")) {
            ParsedFinalizeResult.FinalConclusion finalConclusion =
                    objectMapper.convertValue(rootNode, ParsedFinalizeResult.FinalConclusion.class);
            return new ParsedFinalizeResult(finalConclusion, null, null);
        } else {
            throw new RuntimeException("LLM response missing 'final_conclusion'");
        }
    }
}

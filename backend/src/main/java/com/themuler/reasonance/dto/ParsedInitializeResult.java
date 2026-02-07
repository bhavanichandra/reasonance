package com.themuler.reasonance.dto;

import java.util.List;

public record ParsedInitializeResult(
    String summary,
    List<Conclusion> conclusions,
    List<Evidence> evidences,
    String investigative_direction
) {
    public record Conclusion(
        String conclusion_content,
        String conclusion_summary,
        List<String> tags,
        String confidence
    ) {}

    public record Evidence(
        String evidence_content,
        String evidence_summary,
        List<String> tags,
        String relevance
    ) {}
}

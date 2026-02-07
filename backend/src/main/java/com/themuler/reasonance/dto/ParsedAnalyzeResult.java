package com.themuler.reasonance.dto;

import java.util.List;

public record ParsedAnalyzeResult(
    WhatIf what_if,
    List<Conclusion> conclusions,
    List<Evidence> evidences,
    List<CrossReference> cross_references,
    String investigative_momentum
) {
    public record WhatIf(
        String what_if_content,
        String what_if_summary,
        List<String> tags,
        String confidence,
        List<Integer> references_turns,
        String reasoning_type
    ) {}

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

    public record CrossReference(
        String previous_node_id,
        String relationship,
        String explanation
    ) {}
}

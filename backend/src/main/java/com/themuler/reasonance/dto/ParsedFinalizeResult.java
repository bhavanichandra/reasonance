package com.themuler.reasonance.dto;

import java.util.List;

public record ParsedFinalizeResult(
    FinalConclusion final_conclusion,
    InvestigationSummary investigation_summary,
    Boolean case_closed
) {
    public record FinalConclusion(
        String final_conclusion_content,
        String final_conclusion_summary,
        List<String> tags,
        String confidence,
        Integer key_breakthrough_turn,
        List<Integer> supporting_turns,
        List<AlternativeTheory> alternative_theories_ruled_out
    ) {}

    public record AlternativeTheory(
        String theory,
        String reason_excluded
    ) {}

    public record DecisiveMoment(
        Integer turn,
        String moment
    ) {}

    public record InvestigationSummary(
        Integer total_turns,
        Integer total_hypotheses_explored,
        Integer critical_evidence_count,
        String reasoning_path,
        List<DecisiveMoment> decisive_moments
    ) {}
}

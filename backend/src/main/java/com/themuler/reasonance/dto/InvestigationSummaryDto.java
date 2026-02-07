package com.themuler.reasonance.dto;

import java.util.List;

public record InvestigationSummaryDto(
    Integer totalTurns,
    Integer totalHypothesesExplored,
    Integer criticalEvidenceCount,
    String reasoningPath,
    List<DecisiveMomentDto> decisiveMoments
) {}

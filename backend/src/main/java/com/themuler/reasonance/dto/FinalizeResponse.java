package com.themuler.reasonance.dto;

public record FinalizeResponse(
    FinalConclusionDto finalConclusion,
    InvestigationSummaryDto investigationSummary,
    Boolean caseClosed
) {}

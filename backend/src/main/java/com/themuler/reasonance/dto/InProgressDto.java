package com.themuler.reasonance.dto;

import java.util.List;

public record InProgressDto(
        List<InsightDto> availableInsights,
        List<EvidenceDto> availableEvidences
) {
}

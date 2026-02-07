package com.themuler.reasonance.dto;

import com.themuler.reasonance.entity.ConclusionState;

import java.util.List;

public record InsightDto(
        String id,
        String summary,
        String content,
        int generatedTurn,
        ConclusionState state,
        boolean selectable,
        List<String> tags
) {
}

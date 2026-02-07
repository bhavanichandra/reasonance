package com.themuler.reasonance.dto;

public record InsightSnapshotDto(
        String id,
        String summary,
        String content,
        int generatedTurn
) {
}

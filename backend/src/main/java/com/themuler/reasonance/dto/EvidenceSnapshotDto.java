package com.themuler.reasonance.dto;

public record EvidenceSnapshotDto(
        String id,
        String summary,
        String content,
        int generatedTurn
) {
}

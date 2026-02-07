package com.themuler.reasonance.dto;

public record AnalyzeResponse(
    boolean success,
    int completedTurn,
    Integer nextTurn,
    boolean autoFinalized,
    CompletedSnapshotDto snapshot,
    String finalConclusionId
) {}

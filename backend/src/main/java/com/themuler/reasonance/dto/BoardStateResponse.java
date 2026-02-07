package com.themuler.reasonance.dto;

import java.util.List;
import java.util.Map;

public record BoardStateResponse(
        String spaceId,
        int currentTurn,
        int maxTurns,
        boolean finalized,
        List<TurnDto> turns,
        Map<Integer, List<CompletedSnapshotDto>> completedSnapshotsByTurn,
        InProgressDto inProgress
) {
}

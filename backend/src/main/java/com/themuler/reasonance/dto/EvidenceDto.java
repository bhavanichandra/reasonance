package com.themuler.reasonance.dto;

import com.themuler.reasonance.entity.EvidenceState;

import java.util.List;

public record EvidenceDto(
        String id,
        String summary,
        String content,
        int generatedTurn,
        EvidenceState state,
        Integer usedInTurn,
        boolean draggable,
        List<String> tags
) {
}

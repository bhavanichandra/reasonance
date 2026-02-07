package com.themuler.reasonance.dto;

import com.themuler.reasonance.entity.TurnStatus;

public record TurnDto(
        int turn,
        TurnStatus status
) {
}

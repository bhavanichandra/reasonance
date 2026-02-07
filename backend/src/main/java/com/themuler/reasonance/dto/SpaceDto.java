package com.themuler.reasonance.dto;

import com.themuler.reasonance.entity.SpaceStatus;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class SpaceDto {
    private UUID id;
    private String name;
    private OffsetDateTime createdAt;
    private Integer currentTurn;
    private SpaceStatus status;
}

package com.themuler.reasonance.dto;

import java.util.List;
import java.util.UUID;

public record InitializeResponse(
    UUID spaceId,
    List<UUID> evidenceIds,
    List<UUID> conclusionIds
) {}
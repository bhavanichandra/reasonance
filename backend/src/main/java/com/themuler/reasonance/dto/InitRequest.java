package com.themuler.reasonance.dto;

import jakarta.validation.constraints.NotBlank;

public record InitRequest(
    @NotBlank(message = "Space ID is required")
    String spaceId
) {}
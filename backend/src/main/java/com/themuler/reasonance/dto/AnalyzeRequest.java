package com.themuler.reasonance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AnalyzeRequest(
    @NotBlank(message = "Space ID is required")
    String spaceId,
    
    @NotBlank(message = "Conclusion ID is required")
    String conclusionId,
    
    @NotEmpty(message = "Evidence IDs cannot be empty")
    List<String> evidenceIds,

    @NotNull(message = "Turn is required")
    Integer turn
) {}

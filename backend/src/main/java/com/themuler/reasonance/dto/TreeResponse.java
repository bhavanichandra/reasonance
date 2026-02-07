package com.themuler.reasonance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeResponse {
    private List<BoardNodeDto> tree;
    private Integer currentTurn;
    private FinalConclusionDto finalConclusion;
    private InvestigationSummaryDto investigationSummary;
    private Boolean caseClosed;
}

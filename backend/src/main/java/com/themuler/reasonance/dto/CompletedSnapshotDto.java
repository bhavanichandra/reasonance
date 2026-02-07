package com.themuler.reasonance.dto;

import java.util.List;

public record CompletedSnapshotDto(
        int turn,
        InsightSnapshotDto insight,
        List<EvidenceSnapshotDto> evidences,
        WhatIfSnapshotDto whatIf
) {
}

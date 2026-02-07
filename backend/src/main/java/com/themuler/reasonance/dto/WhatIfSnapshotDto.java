package com.themuler.reasonance.dto;

public record WhatIfSnapshotDto(
        String id,
        String summary,
        String content,
        int generatedTurn
) {
}

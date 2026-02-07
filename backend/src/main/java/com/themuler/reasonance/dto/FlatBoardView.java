package com.themuler.reasonance.dto;

import java.util.UUID;

public record FlatBoardView(
    UUID cId, String cContent, String cSummary, Integer cTurn,
    UUID wId, String wContent, String wSummary, Integer wTurn,
    UUID eId, String eContent, String eSummary, Integer eTurn,
    UUID ncId, String ncContent, String ncSummary, Integer ncTurn
) {}

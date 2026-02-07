package com.themuler.reasonance.dto;

import java.time.Instant;

public record PingResponse(String status, Instant now) {
}

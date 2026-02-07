package com.themuler.reasonance.dto;

public record ServerResult<T>(boolean success, String message, T data) {
}
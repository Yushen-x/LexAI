package com.lexai.backend.common.api;

import java.time.Instant;

public record ApiResponse<T>(
        String code,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "ok", data, Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, T data) {
        return new ApiResponse<>("FAILED", message, data, Instant.now());
    }
}


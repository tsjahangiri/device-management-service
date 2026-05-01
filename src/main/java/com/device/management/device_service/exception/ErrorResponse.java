package com.device.management.device_service.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    private String message;
    private Instant timestamp;

    public ErrorResponse(final String message) {
        this.message = message;
        this.timestamp = Instant.now();
    }
}

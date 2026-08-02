package com.bsplay.shared.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId
) {}

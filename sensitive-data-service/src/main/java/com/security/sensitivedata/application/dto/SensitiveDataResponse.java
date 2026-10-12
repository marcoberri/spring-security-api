package com.security.sensitivedata.application.dto;

import java.time.Instant;
import java.util.UUID;

public record SensitiveDataResponse(
        UUID id,
        String category,
        String payload,
        String secretNote,
        String createdBy,
        Instant createdAt
) {}

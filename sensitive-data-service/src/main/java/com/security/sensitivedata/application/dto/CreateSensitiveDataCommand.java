package com.security.sensitivedata.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSensitiveDataCommand(
        @NotBlank(message = "Category is required")
        @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
        String category,

        @NotBlank(message = "Payload content is required")
        @Size(max = 1000000, message = "Payload content cannot exceed 1MB")
        String payload,

        @Size(max = 500, message = "Secret note cannot exceed 500 characters")
        String secretNote
) {}

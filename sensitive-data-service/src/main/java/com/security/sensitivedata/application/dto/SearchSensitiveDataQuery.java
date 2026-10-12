package com.security.sensitivedata.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SearchSensitiveDataQuery(
        @NotBlank(message = "Category parameter is required")
        @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Category must contain only alphanumeric, underscore or hyphen characters")
        String category
) {}

package com.security.sensitivedata.interfaces.rest;

import com.security.sensitivedata.application.dto.CreateSensitiveDataCommand;
import com.security.sensitivedata.application.dto.SearchSensitiveDataQuery;
import com.security.sensitivedata.application.dto.SensitiveDataResponse;
import com.security.sensitivedata.application.service.SensitiveDataApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sensitive-data")
public class SensitiveDataController {

    private final SensitiveDataApplicationService applicationService;

    public SensitiveDataController(SensitiveDataApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * API 1: Query sensitive data with parameter validation.
     * Validates input query constraints and requires valid authentication.
     */
    @GetMapping("/search")
    public ResponseEntity<List<SensitiveDataResponse>> search(
            @Valid @ModelAttribute SearchSensitiveDataQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = resolveUserId(jwt);
        List<SensitiveDataResponse> results = applicationService.searchByCategory(query, userId);
        return ResponseEntity.ok(results);
    }

    /**
     * Get single record by UUID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SensitiveDataResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = resolveUserId(jwt);
        SensitiveDataResponse result = applicationService.getById(id, userId);
        return ResponseEntity.ok(result);
    }

    /**
     * API 2: Save sensitive information in POST.
     * Validates JSON input structure and ensures max payload size.
     */
    @PostMapping
    public ResponseEntity<SensitiveDataResponse> create(
            @Valid @RequestBody CreateSensitiveDataCommand command,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = resolveUserId(jwt);
        SensitiveDataResponse created = applicationService.save(command, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    private String resolveUserId(Jwt jwt) {
        if (jwt != null) {
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            return jwt.getSubject();
        }
        return "authenticated-user";
    }
}

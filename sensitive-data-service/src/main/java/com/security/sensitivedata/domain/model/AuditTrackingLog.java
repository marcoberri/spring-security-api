package com.security.sensitivedata.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Domain entity for security audit and tracking log.
 * Stores operation type, user identity (extracted from JWT 'sub'), client IP, and timestamp.
 */
@Entity
@Table(name = "audit_tracking_logs")
public class AuditTrackingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(length = 1024)
    private String details;

    protected AuditTrackingLog() {
    }

    public AuditTrackingLog(String operation, String userId, String resource, String status, String details) {
        this.operation = operation;
        this.userId = userId;
        this.resource = resource;
        this.status = status;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOperation() {
        return operation;
    }

    public String getUserId() {
        return userId;
    }

    public String getResource() {
        return resource;
    }

    public String getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}

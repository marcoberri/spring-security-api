package com.security.sensitivedata.domain.model;

import com.security.sensitivedata.infrastructure.crypto.AesGcmCryptoConverter;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate Root representing Sensitive Data entity.
 * Critical fields (payload, secretNote) are encrypted at rest with AES-256-GCM.
 */
@Entity
@Table(name = "sensitive_records")
public class SensitiveRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String category;

    @Convert(converter = AesGcmCryptoConverter.class)
    @Column(nullable = false, length = 4096)
    private String encryptedPayload;

    @Convert(converter = AesGcmCryptoConverter.class)
    @Column(length = 2048)
    private String secretNote;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt;

    protected SensitiveRecord() {
        // JPA requirement
    }

    public SensitiveRecord(String category, String plainPayload, String secretNote, String createdBy) {
        this.category = category;
        this.encryptedPayload = plainPayload;
        this.secretNote = secretNote;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public String getSecretNote() {
        return secretNote;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

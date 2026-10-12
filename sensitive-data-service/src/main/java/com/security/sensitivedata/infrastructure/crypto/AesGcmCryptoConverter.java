package com.security.sensitivedata.infrastructure.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA Attribute Converter implementing AES-256-GCM authenticated encryption.
 * The symmetric key is injected securely via Docker Secrets (or property configuration).
 * Each encryption generates a fresh 12-byte IV prepended to the ciphertext.
 */
@Component
@Converter
public class AesGcmCryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static volatile SecretKey secretKey;
    private static final SecureRandom secureRandom = new SecureRandom();

    public AesGcmCryptoConverter() {
        // JPA/Hibernate spec requires a public no-args constructor
        if (secretKey == null) {
            String fallback = System.getProperty("db_aes_key", System.getenv().getOrDefault("db_aes_key", "DefaultFallbackTestKeyOnly32Bytes!"));
            initKey(fallback);
        }
    }

    public AesGcmCryptoConverter(String rawKey) {
        initKey(rawKey);
    }

    @Value("${app.security.db-aes-key:${db_aes_key:DefaultFallbackTestKeyOnly32Bytes!}}")
    public void setConfiguredKey(String rawKey) {
        initKey(rawKey);
    }

    private synchronized static void initKey(String rawKey) {
        if (secretKey == null) {
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(rawKey.trim());
                if (keyBytes.length != 32) {
                    keyBytes = adjustKeyLength(rawKey.trim().getBytes(StandardCharsets.UTF_8));
                }
            } catch (IllegalArgumentException e) {
                keyBytes = adjustKeyLength(rawKey.trim().getBytes(StandardCharsets.UTF_8));
            }
            secretKey = new SecretKeySpec(keyBytes, "AES");
        }
    }

    private static byte[] adjustKeyLength(byte[] input) {
        byte[] key = new byte[32];
        System.arraycopy(input, 0, key, 0, Math.min(input.length, 32));
        return key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Error encrypting sensitive data attribute with AES-256-GCM", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(dbData);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error decrypting sensitive data attribute with AES-256-GCM", e);
        }
    }
}

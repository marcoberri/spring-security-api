package com.security.sensitivedata.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesGcmCryptoConverterTest {

    @Test
    void shouldEncryptAndDecryptSuccessfullyWithGcm() {
        // Base64 32-byte key
        String testKey = "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=";
        AesGcmCryptoConverter converter = new AesGcmCryptoConverter(testKey);

        String originalData = "{\"ssn\":\"XYZ-998811\",\"amount\":12500.75}";
        String encrypted = converter.convertToDatabaseColumn(originalData);

        assertNotNull(encrypted);
        assertNotEquals(originalData, encrypted);

        // Verification of randomized IV: two encryptions of same text yield different ciphertexts
        String encrypted2 = converter.convertToDatabaseColumn(originalData);
        assertNotEquals(encrypted, encrypted2, "Two encryptions must have different random IVs");

        // Decryption
        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals(originalData, decrypted);
    }
}

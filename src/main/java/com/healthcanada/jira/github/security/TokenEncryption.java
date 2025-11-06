package com.healthcanada.jira.github.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Handles encryption and decryption of sensitive data (tokens, secrets)
 */
public class TokenEncryption {

    private static final Logger log = LoggerFactory.getLogger(TokenEncryption.class);
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 16; // 128-bit AES

    // In production, this should be stored securely (environment variable or Jira system property)
    private static final String DEFAULT_ENCRYPTION_KEY = "github-integration-key-change-me";

    private final String encryptionKey;

    public TokenEncryption() {
        // Try to get encryption key from system property, fall back to default
        this.encryptionKey = System.getProperty("github.integration.encryption.key", DEFAULT_ENCRYPTION_KEY);
        if (DEFAULT_ENCRYPTION_KEY.equals(this.encryptionKey)) {
            log.warn("Using default encryption key. Please set 'github.integration.encryption.key' system property for production.");
        }
    }

    /**
     * Encrypt plaintext string
     */
    public String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            SecretKeySpec key = generateKey(encryptionKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("Failed to encrypt data", e);
            throw new Exception("Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt encrypted string
     */
    public String decrypt(String encryptedText) throws Exception {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            SecretKeySpec key = generateKey(encryptionKey);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt data", e);
            throw new Exception("Decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Generate AES key from encryption key string
     */
    private SecretKeySpec generateKey(String key) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(key.getBytes(StandardCharsets.UTF_8));
        // Use first KEY_SIZE bytes for AES key
        byte[] truncatedKey = Arrays.copyOf(keyBytes, KEY_SIZE);
        return new SecretKeySpec(truncatedKey, ALGORITHM);
    }

    /**
     * Check if a string is encrypted (base64 format check)
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(text);
            // If no exception, it's base64 encoded (likely encrypted)
            return text.length() > 20; // Encrypted tokens are typically long
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

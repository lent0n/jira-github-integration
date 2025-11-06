package com.healthcanada.jira.github.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Validates GitHub webhook signatures using HMAC-SHA256
 */
public class WebhookValidator {

    private static final Logger log = LoggerFactory.getLogger(WebhookValidator.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Verify GitHub webhook signature
     *
     * @param payload   The request body (JSON payload)
     * @param signature The X-Hub-Signature-256 header value
     * @param secret    The webhook secret
     * @return true if signature is valid
     */
    public static boolean verifySignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            log.warn("Webhook validation failed: null parameters");
            return false;
        }

        try {
            // GitHub sends signature as "sha256=<hex_signature>"
            if (!signature.startsWith("sha256=")) {
                log.warn("Invalid signature format: {}", signature);
                return false;
            }

            String expectedSignature = generateSignature(payload, secret);
            String receivedSignature = signature.substring(7); // Remove "sha256=" prefix

            // Use constant-time comparison to prevent timing attacks
            boolean isValid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8)
            );

            if (!isValid) {
                log.warn("Webhook signature mismatch. Expected: sha256={}, Received: {}", expectedSignature, signature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Generate HMAC-SHA256 signature for payload
     *
     * @param payload The request body
     * @param secret  The webhook secret
     * @return Hex-encoded signature
     */
    private static String generateSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Convert byte array to hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Generate a random webhook secret
     */
    public static String generateWebhookSecret() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder secret = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            int index = (int) (Math.random() * characters.length());
            secret.append(characters.charAt(index));
        }
        return secret.toString();
    }
}

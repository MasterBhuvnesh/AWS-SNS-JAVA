package com.otpservice.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OtpStore handles generation, storage, and validation of OTPs.
 *
 * In production, replace the in-memory ConcurrentHashMap with Redis
 * using Spring Data Redis + @Cacheable / RedisTemplate for distributed,
 * TTL-aware OTP storage.
 *
 * Example Redis key: "otp:<phoneNumber>"  →  value: "<otp>:<expiryEpochSec>:<attempts>"
 */
@Component
public class OtpStore {

    @Value("${otp.expiry-minutes}")
    private int expiryMinutes;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${otp.max-attempts}")
    private int maxAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    // Key: phone number, Value: OtpEntry record
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    public record OtpEntry(String otp, Instant expiresAt, int attempts) {}

    /**
     * Generates a new OTP for the given phone number and stores it.
     * Replaces any existing OTP for that number.
     */
    public String generateAndStore(String phoneNumber) {
        String otp = generateNumericOtp();
        Instant expiresAt = Instant.now().plusSeconds(expiryMinutes * 60L);
        store.put(phoneNumber, new OtpEntry(otp, expiresAt, 0));
        return otp;
    }

    /**
     * Validates the OTP for the given phone number.
     * Returns a ValidationResult with outcome reason.
     */
    public ValidationResult validate(String phoneNumber, String inputOtp) {
        OtpEntry entry = store.get(phoneNumber);

        if (entry == null) {
            return ValidationResult.NOT_FOUND;
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(phoneNumber);
            return ValidationResult.EXPIRED;
        }

        if (entry.attempts() >= maxAttempts) {
            store.remove(phoneNumber);
            return ValidationResult.MAX_ATTEMPTS_EXCEEDED;
        }

        if (!entry.otp().equals(inputOtp)) {
            // Increment attempt count
            store.put(phoneNumber, new OtpEntry(entry.otp(), entry.expiresAt(), entry.attempts() + 1));
            return ValidationResult.INVALID;
        }

        // Valid — remove OTP so it can't be reused
        store.remove(phoneNumber);
        return ValidationResult.VALID;
    }

    public void invalidate(String phoneNumber) {
        store.remove(phoneNumber);
    }

    private String generateNumericOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otpInt = secureRandom.nextInt(bound);
        return String.format("%0" + otpLength + "d", otpInt);
    }

    public enum ValidationResult {
        VALID,
        INVALID,
        EXPIRED,
        NOT_FOUND,
        MAX_ATTEMPTS_EXCEEDED
    }
}

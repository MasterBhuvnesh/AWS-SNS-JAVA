package com.otpservice.service;

import com.otpservice.dto.OtpSendResponse;
import com.otpservice.dto.OtpVerifyResponse;
import com.otpservice.util.OtpStore;
import com.otpservice.util.OtpStore.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OtpService orchestrates OTP generation, dispatch, and verification.
 * Depends on OtpStore (state) and SmsService (delivery).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpStore otpStore;
    private final SmsService smsService;

    @Value("${otp.expiry-minutes}")
    private int expiryMinutes;

    /**
     * Generates a new OTP for the phone number and sends it via SMS.
     */
    public OtpSendResponse sendOtp(String phoneNumber) {
        log.info("OTP send requested for phone: {}", maskPhone(phoneNumber));

        String otp = otpStore.generateAndStore(phoneNumber);

        String message = String.format(
                "Your OTP is %s. Valid for %d minute(s). Do not share it with anyone.",
                otp, expiryMinutes
        );

        String messageId = smsService.sendTransactionalSms(phoneNumber, message);

        log.info("OTP sent successfully | Phone: {} | SNS MessageId: {}", maskPhone(phoneNumber), messageId);

        return OtpSendResponse.builder()
                .success(true)
                .message("OTP sent successfully to " + maskPhone(phoneNumber))
                .expiresInMinutes(expiryMinutes)
                .snsMessageId(messageId)
                .build();
    }

    /**
     * Verifies the OTP submitted by the user.
     */
    public OtpVerifyResponse verifyOtp(String phoneNumber, String inputOtp) {
        log.info("OTP verify requested for phone: {}", maskPhone(phoneNumber));

        ValidationResult result = otpStore.validate(phoneNumber, inputOtp);

        return switch (result) {
            case VALID -> {
                log.info("OTP verified successfully for: {}", maskPhone(phoneNumber));
                yield OtpVerifyResponse.builder()
                        .success(true)
                        .message("OTP verified successfully. You are now authenticated.")
                        .build();
            }
            case INVALID -> {
                log.warn("Invalid OTP attempt for: {}", maskPhone(phoneNumber));
                yield OtpVerifyResponse.builder()
                        .success(false)
                        .message("Invalid OTP. Please check and try again.")
                        .build();
            }
            case EXPIRED -> {
                log.warn("Expired OTP for: {}", maskPhone(phoneNumber));
                yield OtpVerifyResponse.builder()
                        .success(false)
                        .message("OTP has expired. Please request a new one.")
                        .build();
            }
            case NOT_FOUND -> {
                log.warn("No OTP found for: {}", maskPhone(phoneNumber));
                yield OtpVerifyResponse.builder()
                        .success(false)
                        .message("No OTP found for this number. Please request a new one.")
                        .build();
            }
            case MAX_ATTEMPTS_EXCEEDED -> {
                log.warn("Max OTP attempts exceeded for: {}", maskPhone(phoneNumber));
                yield OtpVerifyResponse.builder()
                        .success(false)
                        .message("Too many failed attempts. Please request a new OTP.")
                        .build();
            }
        };
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "****";
        return phone.substring(0, 5) + "*****" + phone.substring(phone.length() - 2);
    }
}

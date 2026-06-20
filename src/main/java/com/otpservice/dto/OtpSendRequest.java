package com.otpservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpSendRequest {

    /**
     * Phone number in E.164 format.
     * Examples: +919876543210 (India), +14155552671 (US)
     */
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+[1-9]\\d{6,14}$",
            message = "Phone number must be in E.164 format (e.g. +919876543210)"
    )
    private String phoneNumber;
}

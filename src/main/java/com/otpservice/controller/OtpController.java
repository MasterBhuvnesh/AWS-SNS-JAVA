package com.otpservice.controller;

import com.otpservice.dto.*;
import com.otpservice.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;

    /**
     * POST /api/otp/send
     * Sends an OTP to the given phone number via AWS SNS.
     *
     * Request body:
     * {
     *   "phoneNumber": "+919876543210"
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<OtpSendResponse>> sendOtp(
            @Valid @RequestBody OtpSendRequest request) {

        OtpSendResponse response = otpService.sendOtp(request.getPhoneNumber());

        return ResponseEntity.ok(
                ApiResponse.ok("OTP dispatched", response)
        );
    }

    /**
     * POST /api/otp/verify
     * Verifies the OTP entered by the user.
     *
     * Request body:
     * {
     *   "phoneNumber": "+919876543210",
     *   "otp": "482910"
     * }
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OtpVerifyResponse>> verifyOtp(
            @Valid @RequestBody OtpVerifyRequest request) {

        OtpVerifyResponse response = otpService.verifyOtp(
                request.getPhoneNumber(),
                request.getOtp()
        );

        if (response.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.ok("Verified", response));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.<OtpVerifyResponse>builder()
                        .success(false)
                        .error(response.getMessage())
                        .build());
    }

    /**
     * GET /api/otp/health
     * Basic health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.ok("Service is running", "OK"));
    }
}

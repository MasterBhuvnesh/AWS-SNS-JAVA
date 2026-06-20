package com.otpservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OtpSendResponse {
    private boolean success;
    private String message;
    private Integer expiresInMinutes;
    private String snsMessageId; // useful for tracing/support tickets
}

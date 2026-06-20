package com.otpservice.service;

import com.otpservice.exception.SmsDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.HashMap;
import java.util.Map;

/**
 * SmsService is the sole gateway for outbound SMS.
 * Isolates all AWS SNS logic — swap this class to switch providers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SnsClient snsClient;

    @Value("${otp.sender-id}")
    private String senderId;

    /**
     * Sends a Transactional SMS to the given E.164 phone number.
     * Transactional = high-priority delivery (used for OTPs, alerts).
     *
     * @param phoneNumber E.164 format, e.g. +919876543210
     * @param message     SMS body
     * @return AWS SNS MessageId for tracing
     */
    public String sendTransactionalSms(String phoneNumber, String message) {
        log.info("Sending transactional SMS to {}", maskPhone(phoneNumber));

        Map<String, MessageAttributeValue> attributes = buildSmsAttributes("Transactional");

        PublishRequest request = PublishRequest.builder()
                .phoneNumber(phoneNumber)
                .message(message)
                .messageAttributes(attributes)
                .build();

        try {
            PublishResponse response = snsClient.publish(request);
            String messageId = response.messageId();
            log.info("SMS delivered to {} | MessageId: {}", maskPhone(phoneNumber), messageId);
            return messageId;
        } catch (SnsException e) {
            log.error("SNS delivery failed for {} | Code: {} | Reason: {}",
                    maskPhone(phoneNumber), e.awsErrorDetails().errorCode(), e.getMessage());
            throw new SmsDeliveryException("Failed to deliver SMS: " + e.awsErrorDetails().errorMessage());
        }
    }

    private Map<String, MessageAttributeValue> buildSmsAttributes(String smsType) {
        Map<String, MessageAttributeValue> attrs = new HashMap<>();

        // SMS type: Transactional = OTPs/alerts, Promotional = marketing
        attrs.put("AWS.SNS.SMS.SMSType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(smsType)
                        .build());

        // Sender ID (shown as sender name on device — not supported in all countries/carriers)
        attrs.put("AWS.SNS.SMS.SenderID",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(senderId)
                        .build());

        return attrs;
    }

    /**
     * Masks phone number for safe logging: +91987*****10
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "****";
        return phone.substring(0, 5) + "*****" + phone.substring(phone.length() - 2);
    }
}

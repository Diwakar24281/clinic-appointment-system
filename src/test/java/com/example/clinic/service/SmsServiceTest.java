package com.example.clinic.service;

import com.example.clinic.dto.SmsDiagnosticResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SmsServiceTest {

    private SmsService smsService;

    @BeforeEach
    void setUp() {
        smsService = new SmsService();
    }

    @Test
    @DisplayName("Should detect missing SMS credentials and return MISSING_CREDENTIALS readiness status")
    void testMissingCredentialsReadiness() {
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "");
        ReflectionTestUtils.setField(smsService, "twilioPhoneNumber", "");
        ReflectionTestUtils.setField(smsService, "fast2smsApiKey", "");

        assertFalse(smsService.isConfigured());

        SmsDiagnosticResult result = smsService.checkReadiness();
        assertNotNull(result);
        assertEquals("MISSING_CREDENTIALS", result.getStatus());
        assertFalse(result.isConfigured());
        assertTrue(result.getInstructions().contains("TWILIO_ACCOUNT_SID") || result.getInstructions().contains("FAST2SMS_API_KEY"));
    }

    @Test
    @DisplayName("Should fail fast with SmsDeliveryException when attempting to send OTP without credentials")
    void testSendOtpWithoutCredentials() {
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "");
        ReflectionTestUtils.setField(smsService, "twilioPhoneNumber", "");
        ReflectionTestUtils.setField(smsService, "fast2smsApiKey", "");

        com.example.clinic.exception.SmsDeliveryException ex = assertThrows(
                com.example.clinic.exception.SmsDeliveryException.class, () ->
                smsService.sendOtp("9876543210", "123456", "REGISTRATION")
        );
        assertTrue(ex.getMessage().contains("credentials not configured"));
    }

    @Test
    @DisplayName("Should reject invalid or malformed Indian mobile number")
    void testInvalidMobileNumber() {
        ReflectionTestUtils.setField(smsService, "twilioAccountSid", "AC_dummy_sid");
        ReflectionTestUtils.setField(smsService, "twilioAuthToken", "auth_token_dummy");
        ReflectionTestUtils.setField(smsService, "twilioPhoneNumber", "+1234567890");

        com.example.clinic.exception.BadRequestException ex = assertThrows(
                com.example.clinic.exception.BadRequestException.class, () ->
                smsService.sendOtp("123", "123456", "REGISTRATION")
        );
        assertTrue(ex.getMessage().contains("valid 10-digit Indian mobile number"));
    }

    @Test
    @DisplayName("Should identify Fast2SMS as configured when FAST2SMS_API_KEY is provided")
    void testFast2SmsConfigurationDetection() {
        ReflectionTestUtils.setField(smsService, "fast2smsApiKey", "test_key_dummy_1234567890");
        ReflectionTestUtils.setField(smsService, "configuredProvider", "fast2sms");

        assertTrue(smsService.isConfigured());
        assertEquals("fast2sms", smsService.resolveActiveProvider());
    }

    @Test
    @DisplayName("Should detect invalid Fast2SMS credentials upon readiness verification")
    void testInvalidFast2SmsReadiness() {
        ReflectionTestUtils.setField(smsService, "fast2smsApiKey", "invalid_dummy_key");
        ReflectionTestUtils.setField(smsService, "configuredProvider", "fast2sms");

        SmsDiagnosticResult result = smsService.checkReadiness();
        assertNotNull(result);
        assertEquals("fast2sms", result.getProvider());
        // Since invalid_dummy_key cannot authenticate with Fast2SMS API, it must return AUTHENTICATION_FAILED or API_ERROR
        assertNotEquals("READY", result.getStatus());
    }
}

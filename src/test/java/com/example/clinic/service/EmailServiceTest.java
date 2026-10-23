package com.example.clinic.service;

import com.example.clinic.exception.EmailDeliveryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        emailService.setFromEmail("clinic@gmail.com");
        emailService.setMailPassword("abcdefghijklmnop");
        emailService.setMailHost("smtp.gmail.com");
        emailService.setMailPort("587");
        emailService.setHttpClient(mockHttpClient);
    }

    // =========================================================================
    // 1. SMTP & General Tests (Local Development)
    // =========================================================================

    @Test
    @DisplayName("isSmtpConfigured returns true when credentials are valid")
    void testIsSmtpConfiguredTrue() {
        assertTrue(emailService.isSmtpConfigured());
        assertEquals(EmailService.Provider.SMTP, emailService.getActiveProvider());
    }

    @Test
    @DisplayName("isSmtpConfigured returns false when username or password missing")
    void testIsSmtpConfiguredFalse() {
        emailService.setFromEmail("");
        assertFalse(emailService.isSmtpConfigured());

        emailService.setFromEmail("clinic@gmail.com");
        emailService.setMailPassword("");
        assertFalse(emailService.isSmtpConfigured());
        assertEquals(EmailService.Provider.NONE, emailService.getActiveProvider());
    }

    @Test
    @DisplayName("maskEmail masks email properly")
    void testMaskEmail() {
        assertEquals("r***a@gmail.com", emailService.maskEmail("rohan.sharma@gmail.com"));
        assertEquals("a***@domain.com", emailService.maskEmail("ab@domain.com"));
        assertEquals("****", emailService.maskEmail("invalid-email"));
        assertEquals("****", emailService.maskEmail(null));
    }

    @Test
    @DisplayName("sendRegistrationOtp successfully dispatches email with OTP via SMTP")
    void testSendRegistrationOtpSuccessSmtp() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendRegistrationOtp("patient@example.com", "654321", "Rohan");

        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();

        assertNotNull(sentMessage);
        assertEquals("clinic@gmail.com", sentMessage.getFrom());
        assertArrayEquals(new String[]{"patient@example.com"}, sentMessage.getTo());
        assertTrue(sentMessage.getSubject().contains("Verify Your Email Address"));
        assertTrue(sentMessage.getText().contains("654321"));
        assertTrue(sentMessage.getText().contains("Rohan"));
    }

    @Test
    @DisplayName("sendRegistrationOtp throws EmailDeliveryException when unconfigured")
    void testSendRegistrationOtpUnconfiguredThrows() {
        emailService.setFromEmail("");
        emailService.setMailPassword("");

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendRegistrationOtp("patient@example.com", "654321", "Rohan")
        );
        assertTrue(ex.getMessage().contains("Email service is not configured"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendRegistrationOtp throws clear message on Gmail SMTP authentication failure")
    void testSendRegistrationOtpAuthFailureThrows() {
        doThrow(new MailAuthenticationException("Bad credentials")).when(mailSender).send(any(SimpleMailMessage.class));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendRegistrationOtp("patient@example.com", "654321", "Rohan")
        );
        assertTrue(ex.getMessage().contains("Gmail SMTP Authentication Failed"));
    }

    // =========================================================================
    // 2. Brevo HTTPS REST API Tests (Render Free Deployment)
    // =========================================================================

    @Test
    @DisplayName("Brevo provider is selected when BREVO_API_KEY is configured")
    void testBrevoProviderSelected() {
        emailService.setBrevoApiKey("xkeysib-mock-api-key");
        emailService.setBrevoSenderEmail("doctor@clinic.com");

        assertTrue(emailService.isBrevoConfigured());
        assertEquals(EmailService.Provider.BREVO, emailService.getActiveProvider());
    }

    @Test
    @DisplayName("sendRegistrationOtp dispatches via Brevo HTTPS API without calling SMTP")
    @SuppressWarnings("unchecked")
    void testSendRegistrationOtpSuccessBrevo() throws Exception {
        emailService.setBrevoApiKey("xkeysib-mock-test-key");
        emailService.setBrevoSenderEmail("sender@clinic.com");
        emailService.setBrevoSenderName("Clinic System");

        when(mockHttpResponse.statusCode()).thenReturn(201);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        assertDoesNotThrow(() ->
                emailService.sendRegistrationOtp("patient@example.com", "123456", "Diwakar")
        );

        // Verify HTTP POST request made to Brevo API
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient, times(1)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest capturedRequest = requestCaptor.getValue();

        assertEquals("POST", capturedRequest.method());
        assertEquals("https://api.brevo.com/v3/smtp/email", capturedRequest.uri().toString());
        assertEquals("xkeysib-mock-test-key", capturedRequest.headers().firstValue("api-key").orElse(""));

        // Verify SMTP was NEVER touched
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Brevo API 401 Unauthorized throws clear EmailDeliveryException without falling back to SMTP")
    @SuppressWarnings("unchecked")
    void testBrevoAuthFailureThrowsNoSmtpFallback() throws Exception {
        emailService.setBrevoApiKey("invalid-key");
        when(mockHttpResponse.statusCode()).thenReturn(401);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendRegistrationOtp("patient@example.com", "123456", "Diwakar")
        );

        assertTrue(ex.getMessage().contains("authorization failed"));
        // Strict requirement: Never silently fall back to SMTP when Brevo fails
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Brevo API 429 Rate Limit throws clear EmailDeliveryException")
    @SuppressWarnings("unchecked")
    void testBrevoRateLimitThrows() throws Exception {
        emailService.setBrevoApiKey("xkeysib-valid-key");
        when(mockHttpResponse.statusCode()).thenReturn(429);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendRegistrationOtp("patient@example.com", "123456", "Diwakar")
        );

        assertTrue(ex.getMessage().contains("rate limited"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Brevo HTTP connection timeout throws clear user-facing error")
    @SuppressWarnings("unchecked")
    void testBrevoTimeoutThrows() throws Exception {
        emailService.setBrevoApiKey("xkeysib-valid-key");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpConnectTimeoutException("Connection timed out"));

        EmailDeliveryException ex = assertThrows(EmailDeliveryException.class, () ->
                emailService.sendRegistrationOtp("patient@example.com", "123456", "Diwakar")
        );

        assertTrue(ex.getMessage().contains("timed out"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("sendPasswordResetEmail dispatches via Brevo HTTPS when configured")
    @SuppressWarnings("unchecked")
    void testSendPasswordResetEmailBrevo() throws Exception {
        emailService.setBrevoApiKey("xkeysib-valid-key");
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        boolean result = emailService.sendPasswordResetEmail("patient@example.com", "987654", "Diwakar");
        assertTrue(result);
        verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // =========================================================================
    // 3. Diagnostic Reporting Tests
    // =========================================================================

    @Test
    @DisplayName("getDiagnosticStatus reports safe diagnostic info without password or API keys")
    void testGetDiagnosticStatus() {
        emailService.setBrevoApiKey("xkeysib-secret-token-key");
        emailService.setBrevoSenderEmail("clinic.care@gmail.com");

        Map<String, Object> status = emailService.getDiagnosticStatus();
        assertEquals(true, status.get("configured"));
        assertEquals("READY", status.get("status"));
        assertEquals("BREVO", status.get("activeProvider"));
        assertEquals(true, status.get("brevoConfigured"));
        assertEquals("c***e@gmail.com", status.get("brevoSender"));
        assertEquals(true, status.get("smtpConfigured"));
        assertEquals("c***c@gmail.com", status.get("sender"));

        // Secrets must NEVER be exposed
        assertNull(status.get("password"));
        assertNull(status.get("brevoApiKey"));
        assertNull(status.get("apiKey"));
    }
}

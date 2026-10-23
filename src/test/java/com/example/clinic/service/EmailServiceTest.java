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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        emailService.setFromEmail("clinic@gmail.com");
        emailService.setMailPassword("abcdefghijklmnop");
        emailService.setMailHost("smtp.gmail.com");
        emailService.setMailPort("587");
    }

    @Test
    @DisplayName("isSmtpConfigured returns true when credentials are valid")
    void testIsSmtpConfiguredTrue() {
        assertTrue(emailService.isSmtpConfigured());
    }

    @Test
    @DisplayName("isSmtpConfigured returns false when username or password missing")
    void testIsSmtpConfiguredFalse() {
        emailService.setFromEmail("");
        assertFalse(emailService.isSmtpConfigured());

        emailService.setFromEmail("clinic@gmail.com");
        emailService.setMailPassword("");
        assertFalse(emailService.isSmtpConfigured());
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
    @DisplayName("sendRegistrationOtp successfully dispatches email with OTP")
    void testSendRegistrationOtpSuccess() {
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
        assertTrue(ex.getMessage().contains("Gmail SMTP credentials not configured"));
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

    @Test
    @DisplayName("getDiagnosticStatus reports safe diagnostic info without password")
    void testGetDiagnosticStatus() {
        Map<String, Object> status = emailService.getDiagnosticStatus();
        assertEquals(true, status.get("configured"));
        assertEquals("READY", status.get("status"));
        assertEquals("c***c@gmail.com", status.get("sender"));
        assertEquals("smtp.gmail.com", status.get("host"));
        assertEquals("587", status.get("port"));
        assertNull(status.get("password")); // Password must NEVER be exposed
    }
}

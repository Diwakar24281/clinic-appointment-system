package com.example.clinic.service;

import com.example.clinic.exception.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String fromEmail;

    @Value("${spring.mail.password:${MAIL_PASSWORD:}}")
    private String mailPassword;

    @Value("${spring.mail.host:${SPRING_MAIL_HOST:smtp.gmail.com}}")
    private String mailHost;

    @Value("${spring.mail.port:${SPRING_MAIL_PORT:587}}")
    private String mailPort;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Verifies whether valid Gmail SMTP credentials have been provided in the environment.
     */
    public boolean isSmtpConfigured() {
        return mailSender != null
                && isNotBlank(fromEmail)
                && isNotBlank(mailPassword);
    }

    private boolean isNotBlank(String val) {
        return val != null && !val.trim().isEmpty() && !val.contains("${");
    }

    /**
     * Safely masks an email address for display in logs and responses.
     * Example: "rohan.sharma@gmail.com" -> "r***a@gmail.com"
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name.charAt(0) + "***@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    /**
     * Dispatches a cryptographically secure 6-digit registration OTP to the entered email address.
     * Fails fast with clear diagnostic instructions if Gmail SMTP credentials are not configured or rejected.
     * Note: NEVER logs or exposes the plaintext OTP value.
     */
    public void sendRegistrationOtp(String toEmail, String otp, String recipientName) {
        String maskedDestination = maskEmail(toEmail);
        log.info("Dispatching email registration OTP to destination '{}' via Gmail SMTP host '{}:{}'",
                maskedDestination, mailHost, mailPort);

        if (!isSmtpConfigured()) {
            log.error("Email dispatch aborted: Gmail SMTP credentials (MAIL_USERNAME / MAIL_PASSWORD) not configured.");
            throw new EmailDeliveryException(
                    "Email delivery failed: Gmail SMTP credentials not configured. Please set MAIL_USERNAME and MAIL_PASSWORD in your .env file using your Gmail address and 16-character Google App Password."
            );
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.trim());
            message.setTo(toEmail.trim());
            message.setSubject("🏥 Clinic Appointment System - Verify Your Email Address");

            String name = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : "Valued User";
            message.setText("Dear " + name + ",\n\n"
                    + "Thank you for registering with the Clinic Appointment Booking System.\n\n"
                    + "Your single-use 6-digit email verification code is:\n\n"
                    + "      " + otp + "\n\n"
                    + "This verification code will expire in 5 minutes.\n"
                    + "Please enter this code on the registration page to verify your email and activate your account.\n\n"
                    + "Security Notice:\n"
                    + "• Do not share this OTP with anyone.\n"
                    + "• Clinic staff will never ask for your verification code.\n"
                    + "• If you did not initiate this registration, please disregard this email.\n\n"
                    + "Best regards,\n"
                    + "Clinic Care Team");

            mailSender.send(message);
            log.info("Email verification code successfully dispatched via SMTP to '{}'", maskedDestination);
        } catch (MailAuthenticationException mae) {
            log.error("Gmail SMTP authentication failed for sender '{}': {}", maskEmail(fromEmail), mae.getMessage());
            throw new EmailDeliveryException(
                    "Gmail SMTP Authentication Failed: Invalid credentials. Please ensure MAIL_USERNAME is your full Gmail address and MAIL_PASSWORD is a valid 16-character Google App Password (not your personal account password)."
            );
        } catch (EmailDeliveryException ede) {
            throw ede;
        } catch (Exception ex) {
            log.error("Failed to dispatch email via SMTP to '{}': {}", maskedDestination, ex.getMessage());
            throw new EmailDeliveryException(
                    "Unable to dispatch verification email via Gmail SMTP: " + ex.getMessage(), ex
            );
        }
    }

    /**
     * Backward-compatible boolean helper.
     */
    public boolean sendVerificationEmail(String toEmail, String code, String recipientName) {
        try {
            sendRegistrationOtp(toEmail, code, recipientName);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Sends a password reset email with the single-use 6-digit OTP code.
     */
    public boolean sendPasswordResetEmail(String toEmail, String code, String recipientName) {
        if (!isSmtpConfigured()) {
            log.info("SMTP credentials not set in environment. Code generated securely for '{}'.", maskEmail(toEmail));
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.trim());
            message.setTo(toEmail.trim());
            message.setSubject("🏥 Clinic Appointment System - Reset Your Password");
            message.setText("Dear " + (recipientName != null ? recipientName : "User") + ",\n\n"
                    + "We received a request to reset the password for your Clinic Appointment System account.\n\n"
                    + "Your single-use 6-digit password reset code is:\n\n"
                    + "      " + code + "\n\n"
                    + "This reset code will expire in 5 minutes.\n"
                    + "Enter this code on the password reset page to create your new password.\n\n"
                    + "If you did not request a password reset, please ignore this email or secure your account.\n\n"
                    + "Best regards,\n"
                    + "Clinic Care Team");

            mailSender.send(message);
            log.info("Password reset code successfully dispatched via SMTP to '{}'", maskEmail(toEmail));
            return true;
        } catch (Exception ex) {
            log.error("Failed to dispatch password reset email via SMTP to '{}': {}", maskEmail(toEmail), ex.getMessage());
            return false;
        }
    }

    /**
     * Safe SMTP diagnostic readiness information (does not expose password).
     */
    public Map<String, Object> getDiagnosticStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("configured", isSmtpConfigured());
        status.put("host", mailHost);
        status.put("port", mailPort);
        status.put("sender", isNotBlank(fromEmail) ? maskEmail(fromEmail) : "Not Set");
        status.put("passwordConfigured", isNotBlank(mailPassword));
        status.put("status", isSmtpConfigured() ? "READY" : "MISSING_CREDENTIALS");
        return status;
    }

    // Setters for unit testing
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }

    public void setMailPassword(String mailPassword) {
        this.mailPassword = mailPassword;
    }

    public void setMailHost(String mailHost) {
        this.mailHost = mailHost;
    }

    public void setMailPort(String mailPort) {
        this.mailPort = mailPort;
    }
}

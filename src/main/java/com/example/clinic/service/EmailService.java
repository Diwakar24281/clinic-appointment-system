package com.example.clinic.service;

import com.example.clinic.exception.EmailDeliveryException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public enum Provider {
        BREVO,
        SMTP,
        NONE
    }

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private HttpClient httpClient;

    // Email Provider Selection: auto (prioritizes Brevo if API key present, else SMTP), brevo, or smtp
    @Value("${email.provider:${EMAIL_PROVIDER:auto}}")
    private String emailProvider = "auto";

    // Brevo HTTPS REST API configuration (Port 443 - free tier compatible with Render)
    @Value("${brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:${BREVO_SENDER_EMAIL:${spring.mail.username:${MAIL_USERNAME:}}}}")
    private String brevoSenderEmail;

    @Value("${brevo.sender.name:${BREVO_SENDER_NAME:Clinic Care Team}}")
    private String brevoSenderName = "Clinic Care Team";

    @Value("${brevo.api-url:${BREVO_API_URL:https://api.brevo.com/v3/smtp/email}}")
    private String brevoApiUrl = "https://api.brevo.com/v3/smtp/email";

    // Spring Mail Gmail SMTP configuration (for local development)
    @Value("${spring.mail.username:${MAIL_USERNAME:}}")
    private String fromEmail;

    @Value("${spring.mail.password:${MAIL_PASSWORD:}}")
    private String mailPassword;

    @Value("${spring.mail.host:${SPRING_MAIL_HOST:smtp.gmail.com}}")
    private String mailHost = "smtp.gmail.com";

    @Value("${spring.mail.port:${SPRING_MAIL_PORT:587}}")
    private String mailPort = "587";

    public EmailService(JavaMailSender mailSender) {
        this(mailSender, new ObjectMapper());
    }

    @Autowired
    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        @Autowired(required = false) ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    /**
     * Resolves the active email delivery provider based on configuration.
     */
    public Provider getActiveProvider() {
        if ("brevo".equalsIgnoreCase(emailProvider)) {
            return isBrevoConfigured() ? Provider.BREVO : Provider.NONE;
        } else if ("smtp".equalsIgnoreCase(emailProvider)) {
            return isSmtpConfigured() ? Provider.SMTP : Provider.NONE;
        }

        // "auto" mode: prioritize Brevo if configured (Render HTTPS), otherwise fallback to SMTP
        if (isBrevoConfigured()) {
            return Provider.BREVO;
        }
        if (isSmtpConfigured()) {
            return Provider.SMTP;
        }
        return Provider.NONE;
    }

    /**
     * Verifies whether Brevo HTTPS credentials are present.
     */
    public boolean isBrevoConfigured() {
        return isNotBlank(brevoApiKey);
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
     * Dispatches a 6-digit registration OTP to the entered email address.
     * Routes via Brevo HTTPS REST API (Port 443) on cloud, or Gmail SMTP locally.
     * Note: NEVER logs or exposes the plaintext OTP value or API keys.
     */
    public void sendRegistrationOtp(String toEmail, String otp, String recipientName) {
        String maskedDestination = maskEmail(toEmail);
        Provider provider = getActiveProvider();

        if (provider == Provider.NONE) {
            log.error("Email dispatch aborted: No email provider configured (set BREVO_API_KEY or MAIL_USERNAME/MAIL_PASSWORD).");
            throw new EmailDeliveryException(
                    "Email delivery failed: Email service is not configured. On Render, please set BREVO_API_KEY and BREVO_SENDER_EMAIL. For local development, set MAIL_USERNAME and MAIL_PASSWORD in .env."
            );
        }

        String name = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : "Valued User";
        String subject = "🏥 Clinic Appointment System - Verify Your Email Address";
        String textBody = "Dear " + name + ",\n\n"
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
                + "Clinic Care Team";

        String htmlBody = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;\">"
                + "<h2 style=\"color: #0284c7; margin-top: 0;\">🏥 Clinic Appointment System</h2>"
                + "<p>Dear <strong>" + name + "</strong>,</p>"
                + "<p>Thank you for registering with the Clinic Appointment Booking System.</p>"
                + "<p>Your single-use 6-digit email verification code is:</p>"
                + "<div style=\"background-color: #f0fdf4; border: 1px solid #86efac; border-radius: 6px; padding: 16px; text-align: center; margin: 20px 0;\">"
                + "<span style=\"font-size: 28px; font-weight: 700; letter-spacing: 6px; color: #166534;\">" + otp + "</span>"
                + "</div>"
                + "<p style=\"color: #64748b; font-size: 14px;\">This verification code will expire in <strong>5 minutes</strong>.</p>"
                + "<hr style=\"border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;\">"
                + "<p style=\"color: #94a3b8; font-size: 12px; margin-bottom: 0;\">Security Notice: Do not share this OTP with anyone. Clinic staff will never ask for your code.</p>"
                + "</div>";

        if (provider == Provider.BREVO) {
            sendViaBrevo(toEmail, subject, textBody, htmlBody, name);
        } else {
            sendViaSmtp(toEmail, subject, textBody, maskedDestination);
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
        Provider provider = getActiveProvider();
        if (provider == Provider.NONE) {
            log.info("No email provider configured. Code generated securely for '{}'.", maskEmail(toEmail));
            return false;
        }

        String name = (recipientName != null ? recipientName : "User");
        String subject = "🏥 Clinic Appointment System - Reset Your Password";
        String textBody = "Dear " + name + ",\n\n"
                + "We received a request to reset the password for your Clinic Appointment System account.\n\n"
                + "Your single-use 6-digit password reset code is:\n\n"
                + "      " + code + "\n\n"
                + "This reset code will expire in 5 minutes.\n"
                + "Enter this code on the password reset page to create your new password.\n\n"
                + "If you did not request a password reset, please ignore this email or secure your account.\n\n"
                + "Best regards,\n"
                + "Clinic Care Team";

        String htmlBody = "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;\">"
                + "<h2 style=\"color: #0284c7; margin-top: 0;\">🏥 Clinic Appointment System</h2>"
                + "<p>Dear <strong>" + name + "</strong>,</p>"
                + "<p>We received a request to reset the password for your account.</p>"
                + "<div style=\"background-color: #fef2f2; border: 1px solid #fecaca; border-radius: 6px; padding: 16px; text-align: center; margin: 20px 0;\">"
                + "<span style=\"font-size: 28px; font-weight: 700; letter-spacing: 6px; color: #991b1b;\">" + code + "</span>"
                + "</div>"
                + "<p style=\"color: #64748b; font-size: 14px;\">This code expires in <strong>5 minutes</strong>.</p>"
                + "</div>";

        try {
            if (provider == Provider.BREVO) {
                sendViaBrevo(toEmail, subject, textBody, htmlBody, name);
            } else {
                sendViaSmtp(toEmail, subject, textBody, maskEmail(toEmail));
            }
            return true;
        } catch (Exception ex) {
            log.error("Failed to dispatch password reset email to '{}': {}", maskEmail(toEmail), ex.getMessage());
            return false;
        }
    }

    /**
     * Dispatches email via Brevo HTTPS REST API (Port 443).
     * Strict rule: Never silently falls back to SMTP if Brevo fails.
     */
    private void sendViaBrevo(String toEmail, String subject, String textBody, String htmlBody, String recipientName) {
        String maskedDestination = maskEmail(toEmail);
        String senderEmail = isNotBlank(brevoSenderEmail) ? brevoSenderEmail.trim() : fromEmail;
        if (!isNotBlank(senderEmail)) {
            senderEmail = "no-reply@clinic.com";
        }
        String senderName = isNotBlank(brevoSenderName) ? brevoSenderName.trim() : "Clinic Care Team";
        String recipient = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName.trim() : "Valued User";

        log.info("Dispatching email via Brevo HTTPS REST API to destination '{}'", maskedDestination);

        Map<String, Object> payload = new HashMap<>();
        Map<String, String> senderMap = new HashMap<>();
        senderMap.put("email", senderEmail);
        senderMap.put("name", senderName);
        payload.put("sender", senderMap);

        List<Map<String, String>> toList = new ArrayList<>();
        Map<String, String> toMap = new HashMap<>();
        toMap.put("email", toEmail.trim());
        toMap.put("name", recipient);
        toList.add(toMap);
        payload.put("to", toList);

        payload.put("subject", subject);
        if (htmlBody != null && !htmlBody.trim().isEmpty()) {
            payload.put("htmlContent", htmlBody);
        }
        payload.put("textContent", textBody);

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize Brevo email payload for destination '{}': {}", maskedDestination, e.getMessage());
            throw new EmailDeliveryException("Failed to prepare verification email payload.");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(brevoApiUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int status = response.statusCode();

            if (status == 200 || status == 201 || status == 202) {
                log.info("Email successfully dispatched via Brevo HTTPS API to '{}' (HTTP {})", maskedDestination, status);
            } else if (status == 401 || status == 403) {
                log.error("Brevo API authorization failed (HTTP {}). Please check BREVO_API_KEY.", status);
                throw new EmailDeliveryException("Email service authorization failed. Please check BREVO_API_KEY in environment variables.");
            } else if (status == 429) {
                log.error("Brevo API rate limit exceeded (HTTP 429).");
                throw new EmailDeliveryException("Email service is temporarily rate limited. Please wait a moment and try again.");
            } else {
                log.error("Brevo API delivery rejected with HTTP status {} for destination '{}'", status, maskedDestination);
                throw new EmailDeliveryException("Unable to deliver verification email via Brevo (HTTP " + status + "). Please verify your sender email is verified in Brevo.");
            }
        } catch (HttpConnectTimeoutException e) {
            log.error("Brevo connection timed out for destination '{}'", maskedDestination);
            throw new EmailDeliveryException("Email service connection timed out. Please try again.");
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (Exception e) {
            log.error("Brevo HTTPS API delivery error for destination '{}': {}", maskedDestination, e.getMessage());
            throw new EmailDeliveryException("Email delivery failed via Brevo HTTPS API. Please try again later.");
        }
    }

    /**
     * Dispatches email via Spring Mail SMTP (Gmail SMTP for local dev).
     */
    private void sendViaSmtp(String toEmail, String subject, String textBody, String maskedDestination) {
        log.info("Dispatching email via Gmail SMTP host '{}:{}' to destination '{}'",
                mailHost, mailPort, maskedDestination);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.trim());
            message.setTo(toEmail.trim());
            message.setSubject(subject);
            message.setText(textBody);

            mailSender.send(message);
            log.info("Email successfully dispatched via SMTP to '{}'", maskedDestination);
        } catch (MailAuthenticationException mae) {
            log.error("Gmail SMTP authentication failed for sender '{}': {}", maskEmail(fromEmail), mae.getMessage());
            throw new EmailDeliveryException(
                    "Gmail SMTP Authentication Failed: Invalid credentials. Please ensure MAIL_USERNAME is your full Gmail address and MAIL_PASSWORD is a valid 16-character Google App Password."
            );
        } catch (Exception ex) {
            log.error("Failed to dispatch email via SMTP to '{}': {}", maskedDestination, ex.getMessage());
            throw new EmailDeliveryException(
                    "Unable to dispatch verification email via Gmail SMTP: " + ex.getMessage(), ex
            );
        }
    }

    /**
     * Safe email diagnostic readiness information (does not expose password or API keys).
     */
    public Map<String, Object> getDiagnosticStatus() {
        Map<String, Object> status = new HashMap<>();
        Provider provider = getActiveProvider();
        status.put("activeProvider", provider.name());
        status.put("configured", provider != Provider.NONE);
        status.put("brevoConfigured", isBrevoConfigured());
        status.put("brevoSender", isNotBlank(brevoSenderEmail) ? maskEmail(brevoSenderEmail) : (isNotBlank(fromEmail) ? maskEmail(fromEmail) : "Not Set"));
        status.put("smtpConfigured", isSmtpConfigured());
        status.put("host", mailHost);
        status.put("port", mailPort);
        status.put("sender", isNotBlank(fromEmail) ? maskEmail(fromEmail) : "Not Set");
        status.put("passwordConfigured", isNotBlank(mailPassword));
        status.put("status", provider != Provider.NONE ? "READY" : "MISSING_CREDENTIALS");
        return status;
    }

    // Setters for unit testing
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setBrevoApiKey(String brevoApiKey) {
        this.brevoApiKey = brevoApiKey;
    }

    public void setBrevoSenderEmail(String brevoSenderEmail) {
        this.brevoSenderEmail = brevoSenderEmail;
    }

    public void setBrevoSenderName(String brevoSenderName) {
        this.brevoSenderName = brevoSenderName;
    }

    public void setBrevoApiUrl(String brevoApiUrl) {
        this.brevoApiUrl = brevoApiUrl;
    }

    public void setEmailProvider(String emailProvider) {
        this.emailProvider = emailProvider;
    }

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

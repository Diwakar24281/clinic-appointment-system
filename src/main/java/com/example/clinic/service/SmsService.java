package com.example.clinic.service;

import com.example.clinic.dto.SmsDiagnosticResult;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.SmsDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);
    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile("^(?:\\+91|91|0)?[6-9]\\d{9}$");

    @Value("${sms.provider:${SMS_PROVIDER:twilio}}")
    private String configuredProvider;

    @Value("${twilio.account-sid:${TWILIO_ACCOUNT_SID:}}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:${TWILIO_AUTH_TOKEN:}}")
    private String twilioAuthToken;

    @Value("${twilio.phone-number:${TWILIO_PHONE_NUMBER:}}")
    private String twilioPhoneNumber;

    @Value("${fast2sms.api-key:${FAST2SMS_API_KEY:}}")
    private String fast2smsApiKey;

    @Value("${fast2sms.route:${FAST2SMS_ROUTE:otp}}")
    private String fast2smsRoute;

    private final HttpClient httpClient;

    // Cooldown for diagnostic test messages (system-wide)
    private LocalDateTime lastDiagnosticTestAt = null;

    public SmsService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public SmsService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Normalizes and validates Indian mobile numbers to 10 digits.
     */
    public String normalizeMobile(String inputPhone) {
        if (inputPhone == null || inputPhone.trim().isEmpty()) {
            throw new BadRequestException("Mobile number is required.");
        }

        String cleaned = inputPhone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        if (!INDIAN_PHONE_PATTERN.matcher(cleaned).matches()) {
            throw new BadRequestException("Please enter a valid 10-digit Indian mobile number (e.g. 9876543210).");
        }

        if (cleaned.startsWith("+91")) {
            return cleaned.substring(3);
        } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            return cleaned.substring(1);
        }
        return cleaned;
    }

    /**
     * Determines whether active SMS provider credentials are configured in the environment.
     */
    public boolean isConfigured() {
        String active = resolveActiveProvider();
        if ("fast2sms".equalsIgnoreCase(active)) {
            return isPresent(fast2smsApiKey);
        }
        if ("twilio".equalsIgnoreCase(active)) {
            return isPresent(twilioAccountSid) && isPresent(twilioAuthToken) && isPresent(twilioPhoneNumber);
        }
        return false;
    }

    public String resolveActiveProvider() {
        if (isPresent(fast2smsApiKey) && !isPresent(twilioAccountSid)) {
            return "fast2sms";
        }
        if (isPresent(twilioAccountSid) && isPresent(twilioAuthToken)) {
            return "twilio";
        }
        return (configuredProvider != null && !configuredProvider.trim().isEmpty()) ? configuredProvider.trim().toLowerCase() : "twilio";
    }

    /**
     * Sends the OTP to the mobile number using the configured SMS gateway.
     * Note: NEVER logs the OTP value or returns it.
     */
    public void sendOtp(String mobileNumber, String otp, String purpose) {
        String normalizedMobile = normalizeMobile(mobileNumber);
        String maskedMobile = maskMobile(normalizedMobile);
        String activeProvider = resolveActiveProvider();

        log.info("Dispatching SMS OTP for purpose='{}' to mobile +91 {} via provider '{}'", purpose, maskedMobile, activeProvider);

        if (!isConfigured()) {
            String missingConfigHelp = buildMissingConfigMessage(activeProvider);
            log.error("SMS dispatch aborted: SMS provider credentials not configured in environment. Active provider target: '{}'", activeProvider);
            throw new SmsDeliveryException(missingConfigHelp);
        }

        if ("fast2sms".equalsIgnoreCase(activeProvider)) {
            sendViaFast2Sms(normalizedMobile, otp, purpose);
        } else {
            sendViaTwilio(normalizedMobile, otp, purpose);
        }

        log.info("SMS OTP dispatch accepted by provider '{}' for destination +91 {}", activeProvider, maskedMobile);
    }

    private void sendViaTwilio(String mobile10Digits, String otp, String purpose) {
        String toE164 = "+91" + mobile10Digits;
        String messageBody = buildOtpMessage(otp, purpose);

        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid.trim() + "/Messages.json";
            String formBody = "To=" + URLEncoder.encode(toE164, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(twilioPhoneNumber.trim(), StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            String auth = twilioAccountSid.trim() + ":" + twilioAuthToken.trim();
            String basicAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorDiagnosis = parseTwilioError(response.statusCode(), response.body(), mobile10Digits);
                log.error("Twilio SMS gateway rejected dispatch: {}", errorDiagnosis);
                throw new SmsDeliveryException(errorDiagnosis);
            }
        } catch (SmsDeliveryException sde) {
            throw sde;
        } catch (Exception ex) {
            log.error("Twilio SMS connection failure: {}", ex.getMessage());
            throw new SmsDeliveryException("Unable to contact Twilio SMS gateway: " + ex.getMessage(), ex);
        }
    }

    private void sendViaFast2Sms(String mobile10Digits, String otp, String purpose) {
        try {
            String url = "https://www.fast2sms.com/dev/bulkV2";
            String jsonPayload;
            if ("otp".equalsIgnoreCase(fast2smsRoute)) {
                jsonPayload = "{\"route\":\"otp\",\"variables_values\":\"" + otp + "\",\"numbers\":\"" + mobile10Digits + "\"}";
            } else {
                String message = "Clinic Appointment System: Your OTP is " + otp + ". Valid for 5 minutes.";
                jsonPayload = "{\"route\":\"q\",\"message\":\"" + message + "\",\"language\":\"english\",\"flash\":0,\"numbers\":\"" + mobile10Digits + "\"}";
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("authorization", fast2smsApiKey.trim())
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300 || (response.body() != null && response.body().contains("\"return\":false"))) {
                String errorDiagnosis = parseFast2SmsError(response.statusCode(), response.body());
                log.error("Fast2SMS gateway rejected dispatch: {}", errorDiagnosis);
                throw new SmsDeliveryException(errorDiagnosis);
            }
        } catch (SmsDeliveryException sde) {
            throw sde;
        } catch (Exception ex) {
            log.error("Fast2SMS connection failure: {}", ex.getMessage());
            throw new SmsDeliveryException("Unable to contact Fast2SMS gateway: " + ex.getMessage(), ex);
        }
    }

    /**
     * Extracts rich, actionable diagnostic error details from Twilio's JSON response.
     */
    private String parseTwilioError(int statusCode, String responseBody, String mobile10Digits) {
        String twilioCode = extractJsonStringOrNumber(responseBody, "code");
        String twilioMsg = extractJsonStringOrNumber(responseBody, "message");

        if ("21608".equals(twilioCode)) {
            return "Twilio Trial Restriction (Error 21608): The destination number +91 " + maskMobile(mobile10Digits) 
                    + " is unverified. Twilio trial accounts can only send SMS to numbers verified under 'Verified Caller IDs' in your Twilio Console (twilio.com/console/phone-numbers/verified).";
        }
        if ("20003".equals(twilioCode) || statusCode == 401) {
            return "Twilio Authentication Failed (Error 20003): Invalid TWILIO_ACCOUNT_SID or TWILIO_AUTH_TOKEN. Please verify your credentials in Twilio Console.";
        }
        if ("21211".equals(twilioCode) || "21614".equals(twilioCode)) {
            return "Twilio Invalid Destination (Error " + twilioCode + "): The mobile number +91 " + maskMobile(mobile10Digits) + " is invalid or cannot receive SMS.";
        }
        if ("21408".equals(twilioCode)) {
            return "Twilio Geo-Permission Error (Error 21408): SMS delivery to India (+91) is not enabled on your Twilio account. Please enable India under Twilio SMS Geographic Permissions.";
        }
        if ("21606".equals(twilioCode)) {
            return "Twilio Sender Error (Error 21606): The TWILIO_PHONE_NUMBER is not an active SMS-enabled number in your Twilio account.";
        }

        if (twilioMsg != null && !twilioMsg.trim().isEmpty()) {
            return "Twilio SMS Delivery Failed" + (twilioCode != null ? " (Error " + twilioCode + ")" : "") + ": " + sanitizeForLog(twilioMsg);
        }

        return "Twilio SMS Delivery Failed (HTTP " + statusCode + "). Please check your Twilio account status and phone number configuration.";
    }

    /**
     * Extracts rich, actionable diagnostic error details from Fast2SMS's JSON response.
     */
    private String parseFast2SmsError(int statusCode, String responseBody) {
        String fastMsg = extractJsonStringOrNumber(responseBody, "message");

        if (fastMsg != null) {
            String lower = fastMsg.toLowerCase();
            if (lower.contains("invalid api key") || lower.contains("authorization") || statusCode == 401) {
                return "Fast2SMS Authentication Failed: The FAST2SMS_API_KEY is invalid or inactive. Please copy your active API Key from Fast2SMS Dev API Console (fast2sms.com/dev/api).";
            }
            if (lower.contains("low balance") || lower.contains("insufficient") || lower.contains("recharge")) {
                return "Fast2SMS Account Error: Insufficient SMS wallet balance. Please add funds to your Fast2SMS wallet.";
            }
            if (lower.contains("invalid number")) {
                return "Fast2SMS Phone Error: Invalid Indian mobile number format.";
            }
            return "Fast2SMS Delivery Failed: " + sanitizeForLog(fastMsg);
        }

        return "Fast2SMS Delivery Failed (HTTP " + statusCode + "). Please check your Fast2SMS API key and account status.";
    }

    /**
     * Safe backend readiness check: tests credentials and provider account status
     * without sending any billable SMS.
     */
    public SmsDiagnosticResult checkReadiness() {
        String active = resolveActiveProvider();
        SmsDiagnosticResult result = new SmsDiagnosticResult();
        result.setProvider(active);

        if (!isConfigured()) {
            result.setConfigured(false);
            result.setStatus("MISSING_CREDENTIALS");
            result.setMessage("SMS credentials are not configured in the application environment.");
            result.setInstructions(buildMissingConfigMessage(active));
            return result;
        }

        result.setConfigured(true);
        result.setMaskedKeyOrSid(getMaskedKeyOrSid(active));
        result.setMaskedSender(getMaskedSender(active));

        if ("fast2sms".equalsIgnoreCase(active)) {
            checkFast2SmsReadiness(result);
        } else {
            checkTwilioReadiness(result);
        }

        return result;
    }

    private void checkTwilioReadiness(SmsDiagnosticResult result) {
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioAccountSid.trim() + ".json";
            String auth = twilioAccountSid.trim() + ":" + twilioAuthToken.trim();
            String basicAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String accStatus = extractJsonStringOrNumber(response.body(), "status");
                String accType = extractJsonStringOrNumber(response.body(), "type");

                if ("Trial".equalsIgnoreCase(accType)) {
                    result.setStatus("READY_TRIAL_ACCOUNT");
                    result.setMessage("Twilio credentials authenticated successfully (Trial Account). Destination numbers must be verified under 'Verified Caller IDs' in Twilio Console.");
                    result.setDetails("Account Status: " + accStatus + ", Type: " + accType + ", Sender: " + twilioPhoneNumber);
                } else {
                    result.setStatus("READY");
                    result.setMessage("Twilio gateway is connected, authenticated, and ready to send SMS.");
                    result.setDetails("Account Status: " + accStatus + ", Type: " + accType + ", Sender: " + twilioPhoneNumber);
                }
            } else if (response.statusCode() == 401) {
                result.setStatus("AUTHENTICATION_FAILED");
                result.setMessage("Twilio rejected Account SID or Auth Token. Please check TWILIO_ACCOUNT_SID and TWILIO_AUTH_TOKEN.");
            } else {
                result.setStatus("PROVIDER_ERROR");
                result.setMessage("Twilio API returned HTTP status " + response.statusCode());
            }
        } catch (Exception ex) {
            result.setStatus("NETWORK_ERROR");
            result.setMessage("Unable to reach Twilio API: " + ex.getMessage());
        }
    }

    private void checkFast2SmsReadiness(SmsDiagnosticResult result) {
        try {
            String url = "https://www.fast2sms.com/dev/wallet";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("authorization", fast2smsApiKey.trim())
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && response.body().contains("\"return\":true")) {
                String walletBalance = extractJsonStringOrNumber(response.body(), "wallet");
                result.setStatus("READY");
                result.setMessage("Fast2SMS gateway is connected, authenticated, and ready.");
                result.setDetails("Wallet Balance: Rs " + (walletBalance != null ? walletBalance : "Active") + ", Route: " + fast2smsRoute);
            } else if (response.statusCode() == 401 || (response.body() != null && response.body().contains("Invalid API key"))) {
                result.setStatus("AUTHENTICATION_FAILED");
                result.setMessage("Fast2SMS rejected API Key. Please verify FAST2SMS_API_KEY.");
            } else {
                result.setStatus("PROVIDER_ERROR");
                result.setMessage("Fast2SMS returned response: " + sanitizeForLog(response.body()));
            }
        } catch (Exception ex) {
            result.setStatus("NETWORK_ERROR");
            result.setMessage("Unable to reach Fast2SMS API: " + ex.getMessage());
        }
    }

    /**
     * Executes an authorized single diagnostic test message to verify end-to-end provider acceptance.
     * Rate-limited to max 1 test message per 60 seconds system-wide.
     */
    public synchronized SmsDiagnosticResult sendDiagnosticTest(String testMobile) {
        if (lastDiagnosticTestAt != null && lastDiagnosticTestAt.plusSeconds(60).isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Please wait 60 seconds before sending another diagnostic test message.");
        }

        String normalizedMobile = normalizeMobile(testMobile);
        String testOtp = "123456";

        // Dispatch OTP via live gateway
        sendOtp(normalizedMobile, testOtp, "DIAGNOSTIC_TEST");
        lastDiagnosticTestAt = LocalDateTime.now();

        SmsDiagnosticResult res = new SmsDiagnosticResult();
        res.setProvider(resolveActiveProvider());
        res.setConfigured(true);
        res.setStatus("ACCEPTED_BY_GATEWAY");
        res.setMessage("Diagnostic SMS request accepted by " + resolveActiveProvider() + " gateway for destination +91 " + maskMobile(normalizedMobile) + ".");
        res.setDetails("Note: Provider API acceptance confirms the request was validated and queued for delivery. Handset delivery depends on cellular network carrier and DLT routing.");
        return res;
    }

    private String buildOtpMessage(String otp, String purpose) {
        if ("DIAGNOSTIC_TEST".equalsIgnoreCase(purpose)) {
            return "Clinic Appointment System: Test verification code is " + otp + ". Safe diagnostic readiness verification.";
        }
        String action = "PASSWORD_RESET".equalsIgnoreCase(purpose) ? "reset your password" : "complete your clinic registration";
        return "Clinic Appointment System: Your OTP to " + action + " is " + otp + ". Valid for 5 minutes. Do not share this OTP with anyone.";
    }

    public String maskMobile(String mobile10Digits) {
        if (mobile10Digits == null || mobile10Digits.length() < 10) return "******";
        return "******" + mobile10Digits.substring(mobile10Digits.length() - 4);
    }

    public String getMaskedKeyOrSid(String provider) {
        if ("fast2sms".equalsIgnoreCase(provider) && isPresent(fast2smsApiKey)) {
            String trimmed = fast2smsApiKey.trim();
            if (trimmed.length() <= 8) return "********";
            return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
        }
        if (isPresent(twilioAccountSid)) {
            String trimmed = twilioAccountSid.trim();
            if (trimmed.length() <= 8) return "********";
            return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
        }
        return "Not Set";
    }

    public String getMaskedSender(String provider) {
        if ("fast2sms".equalsIgnoreCase(provider)) {
            return "Fast2SMS (FSTSMS / DLT Route: " + fast2smsRoute + ")";
        }
        return isPresent(twilioPhoneNumber) ? twilioPhoneNumber.trim() : "Not Set";
    }

    private boolean isPresent(String val) {
        return val != null && !val.trim().isEmpty() && !val.contains("${");
    }

    private String sanitizeForLog(String body) {
        if (body == null) return "";
        return body.replaceAll("\\b\\d{6}\\b", "******");
    }

    private String extractJsonStringOrNumber(String json, String fieldName) {
        if (json == null) return null;
        Pattern pattern = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9.]+)|(\\[[^\\]]*\\]))");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            if (matcher.group(1) != null) return matcher.group(1);
            if (matcher.group(2) != null) return matcher.group(2);
            if (matcher.group(3) != null) return matcher.group(3).replaceAll("[\"\\[\\]]", "").trim();
        }
        return null;
    }

    public String buildMissingConfigMessage(String provider) {
        if ("fast2sms".equalsIgnoreCase(provider)) {
            return "SMS delivery failed: Fast2SMS gateway credentials not configured. Please set environment variable FAST2SMS_API_KEY.";
        }
        return "SMS delivery failed: Twilio SMS credentials not configured. Please set environment variables TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_PHONE_NUMBER (or set FAST2SMS_API_KEY).";
    }

    // Setters for test mocks
    public void setConfiguredProvider(String configuredProvider) {
        this.configuredProvider = configuredProvider;
    }

    public void setTwilioAccountSid(String twilioAccountSid) {
        this.twilioAccountSid = twilioAccountSid;
    }

    public void setTwilioAuthToken(String twilioAuthToken) {
        this.twilioAuthToken = twilioAuthToken;
    }

    public void setTwilioPhoneNumber(String twilioPhoneNumber) {
        this.twilioPhoneNumber = twilioPhoneNumber;
    }

    public void setFast2smsApiKey(String fast2smsApiKey) {
        this.fast2smsApiKey = fast2smsApiKey;
    }

    public void setFast2smsRoute(String fast2smsRoute) {
        this.fast2smsRoute = fast2smsRoute;
    }
}

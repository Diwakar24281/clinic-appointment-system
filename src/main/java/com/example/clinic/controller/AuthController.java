package com.example.clinic.controller;

import com.example.clinic.dto.*;
import com.example.clinic.entity.Specialization;
import com.example.clinic.service.AuthService;
import com.example.clinic.service.EmailService;
import com.example.clinic.service.SmsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final SmsService smsService;

    public AuthController(AuthService authService, EmailService emailService, SmsService smsService) {
        this.authService = authService;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    /**
     * Step 1 of Registration: Validates email, generates 6-digit cryptographic OTP,
     * and sends via Gmail SMTP to the entered email address.
     */
    @PostMapping({"/send-registration-otp", "/send-email-otp"})
    public ResponseEntity<AuthResponse> sendRegistrationOtp(@RequestBody SendOtpRequest request) {
        AuthResponse response = authService.sendRegistrationOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2 of Registration: Verifies 6-digit OTP for the email address
     * and returns a single-use verificationToken.
     */
    @PostMapping({"/verify-registration-otp", "/verify-email-otp"})
    public ResponseEntity<AuthResponse> verifyRegistrationOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verifyRegistrationOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 3 of Registration: Creates the patient or doctor account.
     * Strictly requires a verified email OTP session.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/specializations")
    public ResponseEntity<List<Specialization>> getSpecializations() {
        List<Specialization> list = authService.getAllSpecializations();
        return ResponseEntity.ok(list);
    }

    /**
     * Safe Email readiness check endpoint: verifies Gmail SMTP credentials and status
     * without exposing secrets or sending messages.
     */
    @GetMapping("/email-status")
    public ResponseEntity<Map<String, Object>> getEmailStatus() {
        Map<String, Object> result = emailService.getDiagnosticStatus();
        return ResponseEntity.ok(result);
    }

    /**
     * Forgot Password - Step 1: Dispatches SMS OTP to the registered mobile number on record.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        AuthResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Forgot Password - Step 2: Verifies SMS OTP and returns resetToken.
     */
    @PostMapping("/verify-reset-code")
    public ResponseEntity<AuthResponse> verifyResetCode(@RequestBody VerifyResetCodeRequest request) {
        AuthResponse response = authService.verifyResetCode(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Forgot Password - Step 3: Resets password securely with resetToken.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        ApiResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Safe SMS readiness check endpoint: verifies provider credentials and status
     * without exposing secrets or sending billable messages.
     */
    @GetMapping("/sms-status")
    public ResponseEntity<SmsDiagnosticResult> getSmsStatus() {
        SmsDiagnosticResult result = smsService.checkReadiness();
        return ResponseEntity.ok(result);
    }

    /**
     * Safe authorized diagnostic test endpoint: sends a single test message to a specified number.
     * Rate-limited to max 1 test per 60 seconds.
     */
    @PostMapping("/diagnostic/test-sms")
    public ResponseEntity<SmsDiagnosticResult> testSmsDelivery(@RequestBody SendOtpRequest request) {
        SmsDiagnosticResult result = smsService.sendDiagnosticTest(request.getPhone());
        return ResponseEntity.ok(result);
    }
}

package com.example.clinic.controller;

import com.example.clinic.dto.*;
import com.example.clinic.service.AuthService;
import com.example.clinic.service.EmailService;
import com.example.clinic.service.SmsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmsService smsService;

    @Test
    @DisplayName("POST /api/auth/send-registration-otp dispatches email OTP")
    void testSendRegistrationOtpEndpoint() throws Exception {
        SendOtpRequest request = new SendOtpRequest(null, "patient@example.com", "Rohan", "PATIENT", "REGISTRATION");
        AuthResponse response = new AuthResponse(true, "A 6-digit verification code has been dispatched to p***t@example.com", null, "Rohan", "patient@example.com", "PATIENT", null);

        when(authService.sendRegistrationOtp(any(SendOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/send-registration-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.email").value("patient@example.com"))
                .andExpect(jsonPath("$.message").value(response.getMessage()));
    }

    @Test
    @DisplayName("POST /api/auth/send-email-otp alias dispatches email OTP")
    void testSendEmailOtpAliasEndpoint() throws Exception {
        SendOtpRequest request = new SendOtpRequest(null, "patient@example.com", "Rohan", "PATIENT", "REGISTRATION");
        AuthResponse response = new AuthResponse(true, "A 6-digit verification code has been dispatched to p***t@example.com", null, "Rohan", "patient@example.com", "PATIENT", null);

        when(authService.sendRegistrationOtp(any(SendOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/send-email-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/verify-registration-otp verifies code and returns verificationToken")
    void testVerifyRegistrationOtpEndpoint() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest(null, "patient@example.com", "123456", "REGISTRATION");
        AuthResponse response = new AuthResponse(true, "Email address successfully verified!", null, null, "patient@example.com", null, null);
        response.setVerificationToken("token-xyz-123");

        when(authService.verifyRegistrationOtp(any(VerifyOtpRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/verify-registration-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.verificationToken").value("token-xyz-123"));
    }

    @Test
    @DisplayName("POST /api/auth/register creates account and returns 201 CREATED")
    void testRegisterEndpoint() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Rohan Sharma");
        request.setEmail("rohan@example.com");
        request.setPassword("securePass123");
        request.setPhone("9876543210");
        request.setRole("PATIENT");
        request.setVerificationToken("token-xyz-123");

        AuthResponse response = new AuthResponse(true, "Patient account created successfully!", 10L, "Rohan Sharma", "rohan@example.com", "PATIENT", 20L);

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    @DisplayName("GET /api/auth/email-status returns diagnostic readiness info")
    void testGetEmailStatusEndpoint() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("configured", true);
        map.put("host", "smtp.gmail.com");
        map.put("port", "587");
        map.put("sender", "c***c@gmail.com");
        map.put("status", "READY");

        when(emailService.getDiagnosticStatus()).thenReturn(map);

        mockMvc.perform(get("/api/auth/email-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(true))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.host").value("smtp.gmail.com"));
    }
}

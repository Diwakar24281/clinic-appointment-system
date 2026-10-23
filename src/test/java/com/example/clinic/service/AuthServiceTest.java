package com.example.clinic.service;

import com.example.clinic.dto.*;
import com.example.clinic.entity.*;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.EmailDeliveryException;
import com.example.clinic.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private SpecializationRepository specializationRepository;

    @Mock
    private DoctorAvailabilityRepository availabilityRepository;

    @Mock
    private MobileOtpRepository mobileOtpRepository;

    @Mock
    private EmailOtpRepository emailOtpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private AuthService authService;

    private User patientUser;
    private User doctorUser;
    private User adminUser;
    private Patient patientProfile;
    private Doctor doctorProfile;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(1L);
        patientUser.setEmail("patient@clinic.com");
        patientUser.setPhone("9876543210");
        patientUser.setPassword("encodedPassword");
        patientUser.setRole(Role.PATIENT);
        patientUser.setName("John Patient");
        patientUser.setEmailVerified(true);

        patientProfile = new Patient();
        patientProfile.setId(10L);
        patientProfile.setUser(patientUser);
        patientProfile.setPhone("9876543210");

        doctorUser = new User();
        doctorUser.setId(2L);
        doctorUser.setEmail("dr.ramesh@clinic.com");
        doctorUser.setPhone("9876543211");
        doctorUser.setPassword("encodedPassword");
        doctorUser.setRole(Role.DOCTOR);
        doctorUser.setName("Dr. Ramesh");
        doctorUser.setEmailVerified(true);

        doctorProfile = new Doctor();
        doctorProfile.setId(20L);
        doctorProfile.setUser(doctorUser);
        doctorProfile.setApprovalStatus("APPROVED");
        doctorProfile.setPhone("9876543211");

        adminUser = new User();
        adminUser.setId(3L);
        adminUser.setEmail("admin@clinic.com");
        adminUser.setPhone("9876543212");
        adminUser.setPassword("encodedPassword");
        adminUser.setRole(Role.ADMIN);
        adminUser.setName("Clinic Admin");
        adminUser.setEmailVerified(true);
    }

    @Test
    @DisplayName("Patient can login through Patient portal successfully")
    void testPatientLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("patient@clinic.com");
        request.setPassword("password123");
        request.setPortal("PATIENT");

        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(patientUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patientProfile));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("PATIENT", response.getRole());
        assertEquals("John Patient", response.getName());
    }

    @Test
    @DisplayName("Patient cannot login through Doctor portal")
    void testPatientBlockedFromDoctorPortal() {
        LoginRequest request = new LoginRequest();
        request.setEmail("patient@clinic.com");
        request.setPassword("password123");
        request.setPortal("DOCTOR");

        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(patientUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().toLowerCase().contains("doctor portal"));
    }

    @Test
    @DisplayName("Patient cannot login through Admin portal")
    void testPatientBlockedFromAdminPortal() {
        LoginRequest request = new LoginRequest();
        request.setEmail("patient@clinic.com");
        request.setPassword("password123");
        request.setPortal("ADMIN");

        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(patientUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().toLowerCase().contains("admin portal"));
    }

    @Test
    @DisplayName("Doctor cannot login through Patient portal")
    void testDoctorBlockedFromPatientPortal() {
        LoginRequest request = new LoginRequest();
        request.setEmail("dr.ramesh@clinic.com");
        request.setPassword("password123");
        request.setPortal("PATIENT");

        when(userRepository.findByEmail("dr.ramesh@clinic.com")).thenReturn(Optional.of(doctorUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertTrue(ex.getMessage().toLowerCase().contains("patient portal"));
    }

    @Test
    @DisplayName("Doctor can login through Doctor portal successfully")
    void testDoctorLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("dr.ramesh@clinic.com");
        request.setPassword("password123");
        request.setPortal("DOCTOR");

        when(userRepository.findByEmail("dr.ramesh@clinic.com")).thenReturn(Optional.of(doctorUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(doctorRepository.findByUserId(2L)).thenReturn(Optional.of(doctorProfile));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("DOCTOR", response.getRole());
    }

    @Test
    @DisplayName("Admin can login through Admin portal successfully")
    void testAdminLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setEmail("admin@clinic.com");
        request.setPassword("password123");
        request.setPortal("ADMIN");

        when(userRepository.findByEmail("admin@clinic.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    @DisplayName("Invalid password throws BadRequestException")
    void testInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("patient@clinic.com");
        request.setPassword("wrongpassword");
        request.setPortal("PATIENT");

        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(patientUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Registration rejects invalid email format")
    void testRegisterInvalidEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email-address");
        request.setPassword("secret123");
        request.setName("Test User");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("valid original email address"));
    }

    @Test
    @DisplayName("Send Registration OTP succeeds and dispatches via Gmail SMTP")
    void testSendRegistrationOtpSuccess() {
        SendOtpRequest request = new SendOtpRequest(null, "alice@example.com", "Alice Sharma", "PATIENT", "REGISTRATION");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(emailService.maskEmail("alice@example.com")).thenReturn("a***e@example.com");
        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashedOtp");

        AuthResponse response = authService.sendRegistrationOtp(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(emailService, times(1)).sendRegistrationOtp(eq("alice@example.com"), any(String.class), eq("Alice Sharma"));
        verify(emailOtpRepository, times(1)).save(any(EmailOtp.class));
        verify(smsService, never()).sendOtp(any(), any(), any());
    }

    @Test
    @DisplayName("Send Registration OTP rejects already registered email")
    void testSendRegistrationOtpDuplicateEmail() {
        SendOtpRequest request = new SendOtpRequest(null, "patient@clinic.com", "Duplicate User", "PATIENT", "REGISTRATION");
        when(userRepository.existsByEmail("patient@clinic.com")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.sendRegistrationOtp(request));
        assertTrue(ex.getMessage().contains("already registered"));
        verify(emailService, never()).sendRegistrationOtp(any(), any(), any());
    }

    @Test
    @DisplayName("Send Registration OTP enforces 60-second cooldown")
    void testSendRegistrationOtpCooldown() {
        SendOtpRequest request = new SendOtpRequest(null, "alice@example.com", "Alice Sharma", "PATIENT", "REGISTRATION");

        EmailOtp recentOtp = new EmailOtp("alice@example.com", "hash", "REGISTRATION", LocalDateTime.now().plusMinutes(5));
        recentOtp.setLastSentAt(LocalDateTime.now().minusSeconds(20)); // sent 20s ago

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.of(recentOtp));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.sendRegistrationOtp(request));
        assertTrue(ex.getMessage().contains("wait 60 seconds"));
        verify(emailService, never()).sendRegistrationOtp(any(), any(), any());
    }

    @Test
    @DisplayName("Send Registration OTP halts if Gmail SMTP fails")
    void testSendRegistrationOtpSmtpFailure() {
        SendOtpRequest request = new SendOtpRequest(null, "alice@example.com", "Alice Sharma", "PATIENT", "REGISTRATION");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.empty());
        doThrow(new EmailDeliveryException("Gmail SMTP error")).when(emailService).sendRegistrationOtp(eq("alice@example.com"), any(), eq("Alice Sharma"));

        assertThrows(EmailDeliveryException.class, () -> authService.sendRegistrationOtp(request));
        verify(emailOtpRepository, never()).save(any(EmailOtp.class));
    }

    @Test
    @DisplayName("Verify Registration OTP succeeds with correct OTP and returns verificationToken")
    void testVerifyRegistrationOtpSuccess() {
        EmailOtp otpRecord = new EmailOtp("alice@example.com", "hashedCode", "REGISTRATION", LocalDateTime.now().plusMinutes(5));

        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.of(otpRecord));
        when(passwordEncoder.matches("123456", "hashedCode")).thenReturn(true);

        VerifyOtpRequest request = new VerifyOtpRequest(null, "alice@example.com", "123456", "REGISTRATION");
        AuthResponse response = authService.verifyRegistrationOtp(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getVerificationToken());
        assertTrue(otpRecord.isVerified());
        verify(emailOtpRepository, times(1)).save(otpRecord);
    }

    @Test
    @DisplayName("Verify Registration OTP rejects incorrect code and tracks attempts")
    void testVerifyRegistrationOtpIncorrectCode() {
        EmailOtp otpRecord = new EmailOtp("alice@example.com", "hashedCode", "REGISTRATION", LocalDateTime.now().plusMinutes(5));

        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.of(otpRecord));
        when(passwordEncoder.matches("999999", "hashedCode")).thenReturn(false);

        VerifyOtpRequest request = new VerifyOtpRequest(null, "alice@example.com", "999999", "REGISTRATION");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.verifyRegistrationOtp(request));

        assertTrue(ex.getMessage().contains("Incorrect OTP"));
        assertEquals(1, otpRecord.getAttempts());
        assertFalse(otpRecord.isVerified());
    }

    @Test
    @DisplayName("Verify Registration OTP rejects after 5 failed attempts")
    void testVerifyRegistrationOtpExceedsAttempts() {
        EmailOtp otpRecord = new EmailOtp("alice@example.com", "hashedCode", "REGISTRATION", LocalDateTime.now().plusMinutes(5));
        otpRecord.setAttempts(5);

        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.of(otpRecord));

        VerifyOtpRequest request = new VerifyOtpRequest(null, "alice@example.com", "123456", "REGISTRATION");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.verifyRegistrationOtp(request));

        assertTrue(ex.getMessage().contains("Maximum verification attempts exceeded"));
    }

    @Test
    @DisplayName("Verify Registration OTP rejects expired code")
    void testVerifyRegistrationOtpExpired() {
        EmailOtp otpRecord = new EmailOtp("alice@example.com", "hashedCode", "REGISTRATION", LocalDateTime.now().minusMinutes(1)); // expired

        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.of(otpRecord));

        VerifyOtpRequest request = new VerifyOtpRequest(null, "alice@example.com", "123456", "REGISTRATION");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.verifyRegistrationOtp(request));

        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("Verify Registration OTP rejects already verified OTP")
    void testVerifyRegistrationOtpAlreadyVerified() {
        EmailOtp otpRecord = new EmailOtp("alice@example.com", "hashedCode", "REGISTRATION", LocalDateTime.now().plusMinutes(5));
        otpRecord.setVerified(true);

        when(emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc("alice@example.com", "REGISTRATION")).thenReturn(Optional.of(otpRecord));

        VerifyOtpRequest request = new VerifyOtpRequest(null, "alice@example.com", "123456", "REGISTRATION");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.verifyRegistrationOtp(request));

        assertTrue(ex.getMessage().contains("already been verified"));
    }

    @Test
    @DisplayName("Registration succeeds when verified email OTP token is provided")
    void testRegisterPatientWithVerifiedToken() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Sharma");
        request.setEmail("alice@example.com");
        request.setPassword("pass123");
        request.setPhone("9876543210");
        request.setRole("PATIENT");
        request.setVerificationToken("valid-token-123");

        EmailOtp record = new EmailOtp("alice@example.com", "VERIFIED", "REGISTRATION", LocalDateTime.now().plusMinutes(5));
        record.setVerified(true);
        record.setVerificationToken("valid-token-123");
        record.setTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(emailOtpRepository.findByVerificationToken("valid-token-123")).thenReturn(Optional.of(record));
        when(passwordEncoder.encode("pass123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(50L);
            return u;
        });
        when(patientRepository.save(any(Patient.class))).thenAnswer(i -> {
            Patient p = i.getArgument(0);
            p.setId(100L);
            return p;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("PATIENT", response.getRole());
        verify(userRepository).save(any(User.class));
        verify(patientRepository).save(any(Patient.class));
        assertNull(record.getVerificationToken(), "Token should be consumed and invalidated");
    }

    @Test
    @DisplayName("Registration rejects if registration email does not match verified token email")
    void testRegisterEmailMismatchWithVerifiedTokenRejects() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Sharma");
        request.setEmail("different@example.com"); // Mismatched!
        request.setPassword("pass123");
        request.setPhone("9876543210");
        request.setRole("PATIENT");
        request.setVerificationToken("valid-token-123");

        EmailOtp record = new EmailOtp("alice@example.com", "VERIFIED", "REGISTRATION", LocalDateTime.now().plusMinutes(5));
        record.setVerified(true);
        record.setVerificationToken("valid-token-123");
        record.setTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.existsByEmail("different@example.com")).thenReturn(false);
        when(emailOtpRepository.findByVerificationToken("valid-token-123")).thenReturn(Optional.of(record));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("does not match the verified email"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Doctor registration creates PENDING_APPROVAL doctor profile")
    void testRegisterDoctorPendingApproval() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Ramesh Kumar");
        request.setEmail("newdoc@clinic.com");
        request.setPassword("doctorPass123");
        request.setPhone("9876543211");
        request.setRole("DOCTOR");
        request.setVerificationToken("doc-token-123");

        EmailOtp record = new EmailOtp("newdoc@clinic.com", "VERIFIED", "REGISTRATION", LocalDateTime.now().plusMinutes(5));
        record.setVerified(true);
        record.setVerificationToken("doc-token-123");
        record.setTokenExpiry(LocalDateTime.now().plusMinutes(10));

        when(userRepository.existsByEmail("newdoc@clinic.com")).thenReturn(false);
        when(emailOtpRepository.findByVerificationToken("doc-token-123")).thenReturn(Optional.of(record));
        when(passwordEncoder.encode("doctorPass123")).thenReturn("hashedPass");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(60L);
            return u;
        });
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(i -> {
            Doctor d = i.getArgument(0);
            d.setId(120L);
            return d;
        });

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("PENDING_APPROVAL", response.getApprovalStatus());
        verify(doctorRepository).save(argThat(d -> "PENDING_APPROVAL".equals(d.getApprovalStatus()) && !d.isAvailable()));
    }

    @Test
    @DisplayName("Registration is rejected if calling API directly without email OTP verification")
    void testRegisterBlockedWithoutVerifiedOtp() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Hacker");
        request.setEmail("hacker@example.com");
        request.setPassword("pass123");
        request.setPhone("9876543210");
        request.setRole("PATIENT");
        // No verificationToken and no otp provided

        when(userRepository.existsByEmail("hacker@example.com")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertTrue(ex.getMessage().contains("Email OTP verification is required"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Forgot Password dispatches SMS OTP to registered mobile on file")
    void testForgotPasswordWithRegisteredMobile() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("patient@clinic.com");

        when(userRepository.findByEmail("patient@clinic.com")).thenReturn(Optional.of(patientUser));
        when(smsService.normalizeMobile("9876543210")).thenReturn("9876543210");
        when(smsService.maskMobile("9876543210")).thenReturn("******3210");
        when(mobileOtpRepository.findTopByMobileNumberAndPurposeOrderByIdDesc("9876543210", "PASSWORD_RESET")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedResetOtp");

        AuthResponse response = authService.forgotPassword(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        verify(smsService).sendOtp(eq("9876543210"), anyString(), eq("PASSWORD_RESET"));
        verify(mobileOtpRepository).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("Forgot Password fails safely if account has no registered mobile on file")
    void testForgotPasswordAccountWithoutMobileFailsSafely() {
        User userWithoutPhone = new User("No Phone User", "nophone@clinic.com", "hash", Role.PATIENT);
        userWithoutPhone.setPhone(null);

        when(userRepository.findByEmail("nophone@clinic.com")).thenReturn(Optional.of(userWithoutPhone));
        when(patientRepository.findByUserId(userWithoutPhone.getId())).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest("nophone@clinic.com");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.forgotPassword(request));

        assertTrue(ex.getMessage().contains("does not have a registered mobile number on record"));
        verify(smsService, never()).sendOtp(any(), any(), any());
    }
}

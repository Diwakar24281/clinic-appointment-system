package com.example.clinic.service;

import com.example.clinic.dto.*;
import com.example.clinic.entity.*;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final SpecializationRepository specializationRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final MobileOtpRepository mobileOtpRepository;
    private final EmailOtpRepository emailOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    public AuthService(UserRepository userRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       SpecializationRepository specializationRepository,
                       DoctorAvailabilityRepository availabilityRepository,
                       MobileOtpRepository mobileOtpRepository,
                       EmailOtpRepository emailOtpRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       SmsService smsService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.specializationRepository = specializationRepository;
        this.availabilityRepository = availabilityRepository;
        this.mobileOtpRepository = mobileOtpRepository;
        this.emailOtpRepository = emailOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    /**
     * 1. Send OTP to Email for Patient and Doctor Registration.
     * Validates email format and uniqueness, generates a 6-digit cryptographic OTP,
     * dispatches via Gmail SMTP, hashes OTP and persists in email_otps table.
     */
    @Transactional
    public AuthResponse sendRegistrationOtp(SendOtpRequest request) {
        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new BadRequestException("Please enter a valid original email address (e.g. name@domain.com).");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email address '" + normalizedEmail + "' is already registered. Please log in.");
        }

        // Enforce 60-second resend cooldown per email address
        Optional<EmailOtp> existingOpt = emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc(normalizedEmail, "REGISTRATION");
        if (existingOpt.isPresent()) {
            EmailOtp lastOtp = existingOpt.get();
            if (lastOtp.getLastSentAt() != null && lastOtp.getLastSentAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
                throw new BadRequestException("Please wait 60 seconds before requesting another OTP.");
            }
        }

        // Invalidate any previous unverified OTPs for this email
        List<EmailOtp> oldOtps = emailOtpRepository.findByEmailAndPurposeAndVerifiedFalse(normalizedEmail, "REGISTRATION");
        for (EmailOtp old : oldOtps) {
            old.setExpiry(LocalDateTime.now().minusSeconds(1));
            emailOtpRepository.save(old);
        }

        // Generate 6-digit cryptographically secure OTP
        String otp = String.format("%06d", 100000 + RANDOM.nextInt(900000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        // Dispatch via Gmail SMTP - fails cleanly if unconfigured or rejected by mail server
        emailService.sendRegistrationOtp(normalizedEmail, otp, request.getName());

        // Securely hash OTP before database storage (never store plain OTPs)
        String hashedOtp = passwordEncoder.encode(otp);
        EmailOtp record = new EmailOtp(normalizedEmail, hashedOtp, "REGISTRATION", expiry);
        emailOtpRepository.save(record);

        String masked = emailService.maskEmail(normalizedEmail);
        return new AuthResponse(
                true,
                "A 6-digit verification code has been dispatched to " + masked + ". Enter the code to verify your email address.",
                null,
                request.getName(),
                normalizedEmail,
                request.getRole(),
                null
        );
    }

    /**
     * 2. Verify Email OTP for Registration.
     * Enforces expiry (5 min), rate limiting (5 attempts max), single-use verification,
     * invalidates OTP against reuse, and returns a single-use verificationToken for account creation.
     */
    @Transactional
    public AuthResponse verifyRegistrationOtp(VerifyOtpRequest request) {
        if (request.getEmail() == null || request.getOtp() == null) {
            throw new BadRequestException("Email address and 6-digit OTP code are required.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String submittedOtp = request.getOtp().trim();

        EmailOtp record = emailOtpRepository.findTopByEmailAndPurposeOrderByIdDesc(normalizedEmail, "REGISTRATION")
                .orElseThrow(() -> new BadRequestException("No active OTP request found for email " + emailService.maskEmail(normalizedEmail) + ". Please request an OTP."));

        if (record.isVerified()) {
            throw new BadRequestException("This OTP has already been verified and used. Please proceed to create your account.");
        }

        // Rate-limiting attempt count (max 5 attempts)
        if (record.getAttempts() >= 5) {
            throw new BadRequestException("Maximum verification attempts exceeded (5 attempts). Please request a new OTP.");
        }
        record.setAttempts(record.getAttempts() + 1);

        // Expiry check (5 minutes)
        if (record.getExpiry() == null || LocalDateTime.now().isAfter(record.getExpiry())) {
            emailOtpRepository.save(record);
            throw new BadRequestException("Verification OTP has expired (5-minute limit). Please request a fresh OTP.");
        }

        // Hash matching
        if (!passwordEncoder.matches(submittedOtp, record.getOtpHash())) {
            emailOtpRepository.save(record);
            int remaining = 5 - record.getAttempts();
            throw new BadRequestException("Incorrect OTP code. (" + remaining + " attempts remaining).");
        }

        // Success: Mark verified, invalidate code against reuse, issue single-use verificationToken
        record.setVerified(true);
        record.setOtpHash("VERIFIED_" + UUID.randomUUID());
        String verificationToken = UUID.randomUUID().toString();
        record.setVerificationToken(verificationToken);
        record.setTokenExpiry(LocalDateTime.now().plusMinutes(10));
        emailOtpRepository.save(record);

        AuthResponse response = new AuthResponse(
                true,
                "Email address successfully verified! You may now complete account registration.",
                null,
                null,
                record.getEmail(),
                null,
                null
        );
        response.setVerificationToken(verificationToken);
        return response;
    }

    /**
     * 3. Final Account Creation (Patient & Doctor).
     * Strictly requires a verified email OTP session (via verificationToken or valid OTP).
     * Binds the verified email to the account registration.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new BadRequestException("Please enter a valid original email address (e.g. name@domain.com).");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email address '" + normalizedEmail + "' is already registered. Please log in.");
        }

        if (request.getPassword() == null || request.getPassword().trim().length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters long.");
        }

        String roleStr = (request.getRole() != null) ? request.getRole().trim().toUpperCase() : "PATIENT";
        String normalizedPhone = normalizePhone(request.getPhone(), "PATIENT".equals(roleStr));

        // Server-Side Email OTP Verification Check
        validateRegistrationOtpToken(request, normalizedEmail);

        // Create Account
        if ("DOCTOR".equals(roleStr)) {
            return registerDoctorAccount(request, normalizedEmail, normalizedPhone);
        } else {
            return registerPatientAccount(request, normalizedEmail, normalizedPhone);
        }
    }

    private void validateRegistrationOtpToken(RegisterRequest request, String normalizedEmail) {
        if (request.getVerificationToken() != null && !request.getVerificationToken().trim().isEmpty()) {
            String token = request.getVerificationToken().trim();
            EmailOtp record = emailOtpRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new BadRequestException("Invalid or expired email verification token. Please verify your email address."));

            if (!record.isVerified() || !"REGISTRATION".equalsIgnoreCase(record.getPurpose())) {
                throw new BadRequestException("Email address verification is incomplete. Please request and verify an OTP.");
            }

            if (record.getTokenExpiry() == null || LocalDateTime.now().isAfter(record.getTokenExpiry())) {
                throw new BadRequestException("Verification session has expired. Please verify your email address again.");
            }

            // CRITICAL: Ensure verified email matches the registration email
            if (!normalizedEmail.equalsIgnoreCase(record.getEmail())) {
                throw new BadRequestException("Registration email does not match the verified email address.");
            }

            // Invalidate single-use token immediately
            record.setVerificationToken(null);
            record.setTokenExpiry(null);
            emailOtpRepository.save(record);
            return;
        }

        // Direct atomic OTP fallback if provided in the register payload
        if (request.getOtp() != null && !request.getOtp().trim().isEmpty()) {
            VerifyOtpRequest vReq = new VerifyOtpRequest(null, normalizedEmail, request.getOtp().trim(), "REGISTRATION");
            verifyRegistrationOtp(vReq);
            return;
        }

        throw new BadRequestException("Email OTP verification is required before creating an account. Please click 'Send OTP to Email' to verify your email address.");
    }

    private String normalizePhone(String inputPhone, boolean required) {
        if (inputPhone == null || inputPhone.trim().isEmpty()) {
            if (required) {
                throw new BadRequestException("Mobile number is required.");
            }
            return null;
        }

        String cleaned = inputPhone.replaceAll("[\\s\\-\\(\\)]", "").trim();
        if (cleaned.startsWith("+91")) {
            cleaned = cleaned.substring(3);
        } else if (cleaned.startsWith("91") && cleaned.length() == 12) {
            cleaned = cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }

        if (!cleaned.matches("^[6-9]\\d{9}$")) {
            throw new BadRequestException("Please enter a valid 10-digit Indian mobile number (e.g. 9876543210).");
        }
        return cleaned;
    }

    @Transactional
    public AuthResponse registerPatient(RegisterRequest request) {
        return register(request);
    }

    private AuthResponse registerPatientAccount(RegisterRequest request, String email, String phone) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Full name is required for registration.");
        }

        // 1. Create User with activated/verified status and phone stored
        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setRole(Role.PATIENT);
        user.setEmailVerified(true); // Account activated upon email OTP verification
        User savedUser = userRepository.save(user);

        // 2. Create Patient Profile
        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setPhone(phone != null ? phone : "");
        patient.setDob(request.getDob());
        patient.setGender(request.getGender());
        patient.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        Patient savedPatient = patientRepository.save(patient);

        return new AuthResponse(
                true,
                "Patient account created successfully! You can now log in.",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedPatient.getId()
        );
    }

    private AuthResponse registerDoctorAccount(RegisterRequest request, String email, String phone) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("Doctor's full name is required.");
        }

        // 1. Create User with role DOCTOR and phone stored
        User user = new User();
        String docName = request.getName().trim();
        if (!docName.toLowerCase().startsWith("dr.") && !docName.toLowerCase().startsWith("dr ")) {
            docName = "Dr. " + docName;
        }
        user.setName(docName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        user.setRole(Role.DOCTOR);
        user.setEmailVerified(true);
        User savedUser = userRepository.save(user);

        // 2. Create Doctor with PENDING_APPROVAL status (specialization, fee & schedule assigned by Admin)
        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setSpecialization(null); // Assigned by admin during review
        doctor.setConsultationFee(BigDecimal.ZERO); // Set by admin during review
        doctor.setAvailable(false);
        doctor.setApprovalStatus("PENDING_APPROVAL");
        doctor.setPhone(phone);
        Doctor savedDoctor = doctorRepository.save(doctor);

        AuthResponse authResponse = new AuthResponse(
                true,
                "Doctor registration initiated! Your profile is pending administrator review and approval.",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedDoctor.getId()
        );
        authResponse.setApprovalStatus("PENDING_APPROVAL");
        return authResponse;
    }

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }

    /**
     * User Authentication (Patient, Doctor, Admin).
     * Keeps email as the unique login ID.
     */
    public AuthResponse login(LoginRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email address is required.");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Password is required.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password.");
        }

        // Portal role validation
        if (request.getPortal() != null && !request.getPortal().trim().isEmpty()) {
            String requestedPortal = request.getPortal().trim().toUpperCase();
            String userRole = user.getRole().name();
            if (!requestedPortal.equals(userRole)) {
                throw new BadRequestException("Access Denied: You selected the " + requestedPortal 
                        + " portal, but your registered account role is " + userRole + ".");
            }
        }

        Long profileId = null;
        if (user.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found."));
            profileId = patient.getId();
        } else if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
            
            if ("PENDING_APPROVAL".equalsIgnoreCase(doctor.getApprovalStatus())) {
                throw new BadRequestException("Your doctor account is pending administrator review and approval. Please contact clinic management.");
            }
            if ("INACTIVE".equalsIgnoreCase(doctor.getApprovalStatus()) || !doctor.isAvailable()) {
                throw new BadRequestException("Your doctor account is currently inactive. Please contact clinic administration.");
            }
            profileId = doctor.getId();
        }

        return new AuthResponse(
                true,
                "Login successful!",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                profileId
        );
    }

    /**
     * Forgot Password - Step 1: Dispatches an SMS OTP to the registered mobile number on record.
     * Preserved for backward compatibility.
     */
    @Transactional
    public AuthResponse forgotPassword(ForgotPasswordRequest request) {
        if (request.getEmail() == null || !EMAIL_PATTERN.matcher(request.getEmail().trim()).matches()) {
            throw new BadRequestException("Please enter a valid email address.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        Optional<User> optUser = userRepository.findByEmail(normalizedEmail);

        if (optUser.isEmpty()) {
            // Generic safe response to prevent email enumeration
            return new AuthResponse(true, "If an account exists with " + normalizedEmail + ", a password reset code has been dispatched to the registered mobile number.", null, null, normalizedEmail, null, null);
        }

        User user = optUser.get();

        // Doctors must be activated before password reset
        if (user.getRole() == Role.DOCTOR) {
            Doctor doc = doctorRepository.findByUserId(user.getId()).orElse(null);
            if (doc != null && ("PENDING_APPROVAL".equalsIgnoreCase(doc.getApprovalStatus()) || "INACTIVE".equalsIgnoreCase(doc.getApprovalStatus()))) {
                throw new BadRequestException("Doctor account is pending approval or inactive. Please contact clinic management.");
            }
        }

        // Retrieve registered mobile number from existing records
        String registeredPhone = resolveRegisteredPhone(user);
        if (registeredPhone == null || registeredPhone.trim().isEmpty()) {
            throw new BadRequestException("This account does not have a registered mobile number on record. Please contact clinic administration for password recovery.");
        }

        String normalizedPhone = smsService.normalizeMobile(registeredPhone);

        // 60-second cooldown per mobile
        Optional<MobileOtp> existingOpt = mobileOtpRepository.findTopByMobileNumberAndPurposeOrderByIdDesc(normalizedPhone, "PASSWORD_RESET");
        if (existingOpt.isPresent() && existingOpt.get().getLastSentAt() != null && existingOpt.get().getLastSentAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Please wait 60 seconds before requesting another reset code.");
        }

        // Invalidate previous reset OTPs
        List<MobileOtp> oldOtps = mobileOtpRepository.findByMobileNumberAndPurposeAndVerifiedFalse(normalizedPhone, "PASSWORD_RESET");
        for (MobileOtp old : oldOtps) {
            old.setExpiry(LocalDateTime.now().minusSeconds(1));
            mobileOtpRepository.save(old);
        }

        String resetOtp = String.format("%06d", 100000 + RANDOM.nextInt(900000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);

        // Dispatch via SMS gateway
        smsService.sendOtp(normalizedPhone, resetOtp, "PASSWORD_RESET");

        // Save hashed OTP
        String hashedOtp = passwordEncoder.encode(resetOtp);
        MobileOtp record = new MobileOtp(normalizedPhone, normalizedEmail, hashedOtp, "PASSWORD_RESET", expiry);
        mobileOtpRepository.save(record);

        String masked = smsService.maskMobile(normalizedPhone);
        AuthResponse response = new AuthResponse(
                true,
                "A 6-digit password reset code has been sent via SMS to your registered mobile number ending in " + masked.substring(Math.max(0, masked.length() - 4)) + ".",
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                null
        );
        response.setMaskedPhone(masked);
        return response;
    }

    private String resolveRegisteredPhone(User user) {
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            return user.getPhone();
        }
        if (user.getRole() == Role.PATIENT) {
            Optional<Patient> p = patientRepository.findByUserId(user.getId());
            if (p.isPresent() && p.get().getPhone() != null && !p.get().getPhone().trim().isEmpty()) {
                return p.get().getPhone();
            }
        } else if (user.getRole() == Role.DOCTOR) {
            Optional<Doctor> d = doctorRepository.findByUserId(user.getId());
            if (d.isPresent() && d.get().getPhone() != null && !d.get().getPhone().trim().isEmpty()) {
                return d.get().getPhone();
            }
        }
        return null;
    }

    /**
     * Forgot Password - Step 2: Verifies SMS OTP and generates resetToken.
     */
    @Transactional
    public AuthResponse verifyResetCode(VerifyResetCodeRequest request) {
        if (request.getEmail() == null || request.getCode() == null) {
            throw new BadRequestException("Email address and verification code are required.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String submittedCode = request.getCode().trim();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email address or reset request."));

        MobileOtp record = mobileOtpRepository.findTopByEmailAndPurposeOrderByIdDesc(normalizedEmail, "PASSWORD_RESET")
                .orElseThrow(() -> new BadRequestException("No active password reset request found for this account."));

        if (record.isVerified()) {
            throw new BadRequestException("This code has already been verified. Please proceed to set your new password.");
        }

        if (record.getAttempts() >= 5) {
            throw new BadRequestException("Verification attempt limit exceeded (5 attempts). Please request a new code.");
        }
        record.setAttempts(record.getAttempts() + 1);

        if (record.getExpiry() == null || LocalDateTime.now().isAfter(record.getExpiry())) {
            mobileOtpRepository.save(record);
            throw new BadRequestException("Password reset code has expired (5-minute limit). Please request a new code.");
        }

        if (!passwordEncoder.matches(submittedCode, record.getOtpHash())) {
            mobileOtpRepository.save(record);
            int remaining = 5 - record.getAttempts();
            throw new BadRequestException("Incorrect reset code. (" + remaining + " attempts remaining).");
        }

        // Success: Invalidate OTP and issue secure single-use reset authorization token
        record.setVerified(true);
        record.setOtpHash("VERIFIED_" + UUID.randomUUID());
        mobileOtpRepository.save(record);

        String resetToken = UUID.randomUUID().toString();
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        AuthResponse resp = new AuthResponse(true, "Code verified successfully! You may now enter your new password.", user.getId(), user.getName(), user.getEmail(), user.getRole().name(), null);
        resp.setResetToken(resetToken);
        return resp;
    }

    /**
     * Forgot Password - Step 3: Resets password securely using verified resetToken.
     */
    @Transactional
    public com.example.clinic.dto.ApiResponse resetPassword(ResetPasswordRequest request) {
        if (request.getEmail() == null || request.getResetToken() == null || request.getNewPassword() == null) {
            throw new BadRequestException("Email, reset token, and new password are required.");
        }

        if (request.getNewPassword().trim().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long.");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid reset request."));

        if (user.getResetToken() == null || !user.getResetToken().equals(request.getResetToken().trim())) {
            throw new BadRequestException("Invalid or expired password reset token. Please request a new code.");
        }

        if (user.getResetTokenExpiry() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiry())) {
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            throw new BadRequestException("Password reset session has expired. Please start over.");
        }

        // Securely hash and update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        return new com.example.clinic.dto.ApiResponse(true, "Password has been reset successfully! You can now sign in with your new password.");
    }
}

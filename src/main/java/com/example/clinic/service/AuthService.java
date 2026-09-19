package com.example.clinic.service;

import com.example.clinic.dto.AuthResponse;
import com.example.clinic.dto.LoginRequest;
import com.example.clinic.dto.RegisterRequest;
import com.example.clinic.entity.Doctor;
import com.example.clinic.entity.Patient;
import com.example.clinic.entity.Role;
import com.example.clinic.entity.User;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.repository.DoctorRepository;
import com.example.clinic.repository.PatientRepository;
import com.example.clinic.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse registerPatient(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered. Please log in.");
        }

        // 1. Create and save User with hashed password
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.PATIENT);
        User savedUser = userRepository.save(user);

        // 2. Create and save Patient profile
        Patient patient = new Patient();
        patient.setUser(savedUser);
        patient.setPhone(request.getPhone());
        patient.setDob(request.getDob());
        patient.setGender(request.getGender());
        patient.setAddress(request.getAddress());
        Patient savedPatient = patientRepository.save(patient);

        return new AuthResponse(
                true,
                "Registration successful! Please login.",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedPatient.getId()
        );
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password.");
        }

        Long profileId = null;
        if (user.getRole() == Role.PATIENT) {
            Patient patient = patientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found."));
            profileId = patient.getId();
        } else if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found."));
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
}

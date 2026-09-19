package com.example.clinic.service;

import com.example.clinic.dto.*;
import com.example.clinic.entity.*;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final SpecializationRepository specializationRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final PasswordEncoder passwordEncoder;

    public AdminService(DoctorRepository doctorRepository,
                        PatientRepository patientRepository,
                        UserRepository userRepository,
                        SpecializationRepository specializationRepository,
                        DoctorAvailabilityRepository availabilityRepository,
                        AppointmentRepository appointmentRepository,
                        DoctorService doctorService,
                        AppointmentService appointmentService,
                        PasswordEncoder passwordEncoder) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.specializationRepository = specializationRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminStatsResponse getStats() {
        long totalDoctors = doctorRepository.count();
        long totalPatients = patientRepository.count();
        long todayAppointments = appointmentRepository.findAll().stream()
                .filter(a -> a.getAppointmentDate().isEqual(LocalDate.now()))
                .count();
        long totalCompleted = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        return new AdminStatsResponse(totalDoctors, totalPatients, todayAppointments, totalCompleted);
    }

    @Transactional
    public DoctorDto createDoctor(DoctorCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered in the system.");
        }

        Specialization spec = specializationRepository.findById(request.getSpecializationId())
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with ID: " + request.getSpecializationId()));

        // 1. Create User
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.DOCTOR);
        User savedUser = userRepository.save(user);

        // 2. Create Doctor
        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setSpecialization(spec);
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setAvailable(true);
        Doctor savedDoctor = doctorRepository.save(doctor);

        // 3. Save Availabilities if provided
        if (request.getAvailabilities() != null) {
            for (DoctorAvailabilityDto aDto : request.getAvailabilities()) {
                DoctorAvailability da = new DoctorAvailability(
                        savedDoctor,
                        aDto.getDayOfWeek(),
                        aDto.getStartTime(),
                        aDto.getEndTime()
                );
                availabilityRepository.save(da);
            }
        }

        return doctorService.mapToDto(savedDoctor);
    }

    @Transactional
    public DoctorDto updateDoctor(Long doctorId, DoctorUpdateRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            doctor.getUser().setName(request.getName().trim());
        }

        if (request.getSpecializationId() != null) {
            Specialization spec = specializationRepository.findById(request.getSpecializationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with ID: " + request.getSpecializationId()));
            doctor.setSpecialization(spec);
        }

        if (request.getConsultationFee() != null) {
            doctor.setConsultationFee(request.getConsultationFee());
        }

        if (request.getIsAvailable() != null) {
            doctor.setAvailable(request.getIsAvailable());
        }

        Doctor updated = doctorRepository.save(doctor);

        // Update Availabilities if provided
        if (request.getAvailabilities() != null) {
            availabilityRepository.deleteByDoctorId(doctorId);
            for (DoctorAvailabilityDto aDto : request.getAvailabilities()) {
                DoctorAvailability da = new DoctorAvailability(
                        updated,
                        aDto.getDayOfWeek(),
                        aDto.getStartTime(),
                        aDto.getEndTime()
                );
                availabilityRepository.save(da);
            }
        }

        return doctorService.mapToDto(updated);
    }

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }

    @Transactional
    public Specialization createSpecialization(Specialization specialization) {
        if (specializationRepository.existsByNameIgnoreCase(specialization.getName())) {
            throw new BadRequestException("Specialization already exists with name: " + specialization.getName());
        }
        return specializationRepository.save(specialization);
    }

    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(appointmentService::mapToResponse)
                .collect(Collectors.toList());
    }
}

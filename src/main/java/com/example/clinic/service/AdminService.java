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

        // 3. Save Availabilities if provided, else assign default Mon-Sat 9AM-10PM
        if (request.getAvailabilities() != null && !request.getAvailabilities().isEmpty()) {
            for (DoctorAvailabilityDto aDto : request.getAvailabilities()) {
                DoctorAvailability da = new DoctorAvailability(
                        savedDoctor,
                        aDto.getDayOfWeek(),
                        aDto.getStartTime(),
                        aDto.getEndTime()
                );
                availabilityRepository.save(da);
            }
        } else {
            java.time.DayOfWeek[] defaultDays = {
                    java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY,
                    java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY
            };
            for (java.time.DayOfWeek day : defaultDays) {
                availabilityRepository.save(new DoctorAvailability(
                        savedDoctor,
                        day,
                        java.time.LocalTime.of(9, 0),
                        java.time.LocalTime.of(22, 0)
                ));
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

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            doctor.setPhone(request.getPhone().trim());
        }

        if (request.getApprovalStatus() != null && !request.getApprovalStatus().trim().isEmpty()) {
            String newStatus = request.getApprovalStatus().trim().toUpperCase();
            doctor.setApprovalStatus(newStatus);
            if ("APPROVED".equals(newStatus)) {
                doctor.setAvailable(true);
            } else if ("INACTIVE".equals(newStatus)) {
                doctor.setAvailable(false);
            }
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
        } else if ("APPROVED".equals(updated.getApprovalStatus())) {
            // If approved and no availability records exist, assign default 9 AM - 10 PM
            List<DoctorAvailability> existing = availabilityRepository.findByDoctorId(doctorId);
            if (existing.isEmpty()) {
                java.time.DayOfWeek[] defaultDays = {
                        java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY,
                        java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY
                };
                for (java.time.DayOfWeek day : defaultDays) {
                    availabilityRepository.save(new DoctorAvailability(
                            updated,
                            day,
                            java.time.LocalTime.of(9, 0),
                            java.time.LocalTime.of(22, 0)
                    ));
                }
            }
        }

        return doctorService.mapToDto(updated);
    }

    public List<Specialization> getAllSpecializations() {
        return specializationRepository.findAll();
    }

    @Transactional
    public Specialization createSpecialization(Specialization specialization) {
        if (specialization.getName() == null || specialization.getName().trim().isEmpty()) {
            throw new BadRequestException("Specialization name is required.");
        }
        if (specializationRepository.existsByNameIgnoreCase(specialization.getName().trim())) {
            throw new BadRequestException("Specialization already exists with name: " + specialization.getName().trim());
        }
        specialization.setName(specialization.getName().trim());
        if (specialization.getDescription() != null) {
            specialization.setDescription(specialization.getDescription().trim());
        }
        return specializationRepository.save(specialization);
    }

    @Transactional
    public void deleteSpecialization(Long id) {
        Specialization spec = specializationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Specialization not found with ID: " + id));

        // Referential integrity check: Check if any doctor is assigned this specialization
        boolean assignedToDoctor = doctorRepository.findAll().stream()
                .anyMatch(d -> d.getSpecialization() != null && d.getSpecialization().getId().equals(id));
        if (assignedToDoctor) {
            throw new BadRequestException("Cannot delete medical specialization '" + spec.getName() 
                    + "' because it is currently assigned to one or more doctors. Please reassign doctors before deleting.");
        }

        // Referential integrity check: Check if any historical appointment exists under this specialization
        boolean assignedToAppointment = appointmentRepository.findAll().stream()
                .anyMatch(a -> a.getDoctor() != null && a.getDoctor().getSpecialization() != null 
                        && a.getDoctor().getSpecialization().getId().equals(id));
        if (assignedToAppointment) {
            throw new BadRequestException("Cannot delete medical specialization '" + spec.getName() 
                    + "' because historical appointments exist under this department.");
        }

        specializationRepository.delete(spec);
    }

    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(appointmentService::mapToResponse)
                .collect(Collectors.toList());
    }
}

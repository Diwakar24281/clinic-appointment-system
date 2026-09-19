package com.example.clinic.service;

import com.example.clinic.dto.ApiResponse;
import com.example.clinic.dto.AppointmentRequest;
import com.example.clinic.dto.AppointmentResponse;
import com.example.clinic.entity.Appointment;
import com.example.clinic.entity.AppointmentStatus;
import com.example.clinic.entity.Doctor;
import com.example.clinic.entity.Patient;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.exception.SlotAlreadyBookedException;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.DoctorRepository;
import com.example.clinic.repository.PatientRepository;
import com.example.clinic.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              PrescriptionRepository prescriptionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        // 1. Validate Doctor & Patient
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        if (!doctor.isAvailable()) {
            throw new BadRequestException("Doctor is currently not available for bookings.");
        }

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        // 2. Prevent booking in the past
        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot book appointment for a past date.");
        }

        // 3. STRICT DOUBLE-BOOKING CHECK
        boolean isAlreadyBooked = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                doctor.getId(),
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                Arrays.asList(AppointmentStatus.BOOKED, AppointmentStatus.IN_CONSULTATION)
        );

        if (isAlreadyBooked) {
            throw new SlotAlreadyBookedException("Selected appointment slot is already booked. Please choose another time.");
        }

        // 4. GENERATE SEQUENTIAL TOKEN NUMBER (Isolated per doctor and date)
        int existingCount = appointmentRepository.countByDoctorIdAndAppointmentDate(doctor.getId(), request.getAppointmentDate());
        int tokenNumber = existingCount + 1;

        // 5. Create and save Appointment
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setAppointmentTime(request.getAppointmentTime());
        appointment.setTokenNumber(tokenNumber);
        appointment.setStatus(AppointmentStatus.BOOKED);

        Appointment saved = appointmentRepository.save(appointment);

        return mapToResponse(saved);
    }

    public List<AppointmentResponse> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AppointmentResponse> getDoctorAppointments(Long doctorId, LocalDate date) {
        LocalDate searchDate = (date != null) ? date : LocalDate.now();
        return appointmentRepository.findByDoctorIdAndAppointmentDateOrderByTokenNumberAsc(doctorId, searchDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new BadRequestException("Only pending (BOOKED) appointments can be cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);

        return new ApiResponse(true, "Appointment cancelled successfully.");
    }

    public AppointmentResponse getAppointmentById(Long id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
        return mapToResponse(appointment);
    }

    public AppointmentResponse mapToResponse(Appointment apt) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(apt.getId());
        res.setPatientId(apt.getPatient().getId());
        res.setPatientName(apt.getPatient().getUser().getName());
        res.setPatientPhone(apt.getPatient().getPhone());
        res.setDoctorId(apt.getDoctor().getId());
        res.setDoctorName(apt.getDoctor().getUser().getName());
        res.setSpecialization(apt.getDoctor().getSpecialization().getName());
        res.setConsultationFee(apt.getDoctor().getConsultationFee());
        res.setAppointmentDate(apt.getAppointmentDate());
        res.setAppointmentTime(apt.getAppointmentTime());
        res.setDisplayTime(apt.getAppointmentTime().format(TIME_FORMATTER));
        res.setTokenNumber(apt.getTokenNumber());
        res.setStatus(apt.getStatus());

        prescriptionRepository.findByAppointmentId(apt.getId())
                .ifPresent(p -> res.setPrescriptionId(p.getId()));

        return res;
    }
}

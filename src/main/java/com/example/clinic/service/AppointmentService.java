package com.example.clinic.service;

import com.example.clinic.dto.ApiResponse;
import com.example.clinic.dto.AppointmentRequest;
import com.example.clinic.dto.AppointmentResponse;
import com.example.clinic.entity.Appointment;
import com.example.clinic.entity.AppointmentStatus;
import com.example.clinic.entity.Doctor;
import com.example.clinic.entity.Patient;
import com.example.clinic.entity.Role;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.exception.SlotAlreadyBookedException;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.DoctorRepository;
import com.example.clinic.repository.PatientRepository;
import com.example.clinic.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.clinic.entity.DoctorAvailability;
import com.example.clinic.repository.DoctorAvailabilityRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    public static final LocalTime LUNCH_START = LocalTime.of(13, 0); // 1:00 PM
    public static final LocalTime LUNCH_END = LocalTime.of(14, 30);   // 2:30 PM
    public static final int SLOT_DURATION_MINUTES = 30;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              DoctorAvailabilityRepository availabilityRepository,
                              PatientRepository patientRepository,
                              PrescriptionRepository prescriptionRepository) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.patientRepository = patientRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        // 1. Validate Doctor & Patient existence
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        if ("PENDING_APPROVAL".equalsIgnoreCase(doctor.getApprovalStatus()) || "INACTIVE".equalsIgnoreCase(doctor.getApprovalStatus())) {
            throw new BadRequestException("Doctor is not currently activated for appointments.");
        }

        if (!doctor.isAvailable()) {
            throw new BadRequestException("Doctor is currently on leave / unavailable for bookings.");
        }

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        if (patient.getUser() != null && patient.getUser().getRole() != Role.PATIENT) {
            throw new BadRequestException("Access Denied: Only registered Patient accounts can book appointments.");
        }

        LocalDate appointmentDate = request.getAppointmentDate();
        LocalTime appointmentTime = request.getAppointmentTime();

        // 2. Prevent booking in the past
        LocalDate today = LocalDate.now();
        if (appointmentDate.isBefore(today)) {
            throw new BadRequestException("Cannot book an appointment for a past date.");
        }
        if (appointmentDate.isEqual(today) && appointmentTime.isBefore(LocalTime.now())) {
            throw new BadRequestException("Cannot book an appointment slot that has already passed.");
        }

        // 3. Validate 30-minute slot alignment
        if (appointmentTime.getMinute() != 0 && appointmentTime.getMinute() != 30) {
            throw new BadRequestException("Appointments must be scheduled on the hour or half-hour (e.g. 09:00, 09:30).");
        }
        if (appointmentTime.getSecond() != 0 || appointmentTime.getNano() != 0) {
            appointmentTime = LocalTime.of(appointmentTime.getHour(), appointmentTime.getMinute());
        }

        // 4. Validate against Doctor's Schedule on that Day
        DayOfWeek dayOfWeek = appointmentDate.getDayOfWeek();
        Optional<DoctorAvailability> optAvailability = availabilityRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), dayOfWeek);

        if (optAvailability.isEmpty()) {
            throw new BadRequestException("Doctor is not scheduled for consultations on " + dayOfWeek.name() + ".");
        }

        DoctorAvailability availability = optAvailability.get();
        LocalTime docStart = availability.getStartTime();
        LocalTime docEnd = availability.getEndTime();
        LocalTime slotEnd = appointmentTime.plusMinutes(SLOT_DURATION_MINUTES);

        // Check if slot falls outside working hours
        if (appointmentTime.isBefore(docStart)) {
            throw new BadRequestException("Requested time (" + appointmentTime.format(TIME_FORMATTER) 
                    + ") is before doctor's consultation start time (" + docStart.format(TIME_FORMATTER) + ").");
        }
        if (appointmentTime.isAfter(docEnd) || slotEnd.isAfter(docEnd)) {
            throw new BadRequestException("Requested appointment ends at " + slotEnd.format(TIME_FORMATTER) 
                    + ", which extends past doctor's scheduled closing time (" + docEnd.format(TIME_FORMATTER) + ").");
        }

        // 5. Check Lunch Break (1:00 PM to 2:30 PM)
        boolean startsInLunch = !appointmentTime.isBefore(LUNCH_START) && appointmentTime.isBefore(LUNCH_END);
        boolean overlapsLunch = appointmentTime.isBefore(LUNCH_START) && slotEnd.isAfter(LUNCH_START);
        if (startsInLunch || overlapsLunch) {
            throw new BadRequestException("Appointments cannot be booked during the clinic lunch break (01:00 PM - 02:30 PM).");
        }

        // 6. STRICT DOUBLE-BOOKING CHECK
        boolean isAlreadyBooked = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(
                doctor.getId(),
                appointmentDate,
                appointmentTime,
                Arrays.asList(AppointmentStatus.BOOKED, AppointmentStatus.IN_CONSULTATION)
        );

        if (isAlreadyBooked) {
            throw new SlotAlreadyBookedException("Selected appointment slot (" + appointmentTime.format(TIME_FORMATTER) + ") is already booked. Please choose another time.");
        }

        // 7. GENERATE SEQUENTIAL TOKEN NUMBER (Isolated per doctor and date)
        int existingCount = appointmentRepository.countByDoctorIdAndAppointmentDate(doctor.getId(), appointmentDate);
        int tokenNumber = existingCount + 1;

        // 8. Create and save Appointment with concurrency safety
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setTokenNumber(tokenNumber);
        appointment.setStatus(AppointmentStatus.BOOKED);

        Appointment saved;
        try {
            saved = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new SlotAlreadyBookedException("Concurrency conflict: The selected slot was just booked by another patient. Please choose another slot.");
        }

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

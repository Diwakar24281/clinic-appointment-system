package com.example.clinic.service;

import com.example.clinic.dto.AppointmentResponse;
import com.example.clinic.dto.QueueStatusResponse;
import com.example.clinic.entity.Appointment;
import com.example.clinic.entity.AppointmentStatus;
import com.example.clinic.entity.Doctor;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class QueueService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentService appointmentService;

    public QueueService(AppointmentRepository appointmentRepository,
                        DoctorRepository doctorRepository,
                        AppointmentService appointmentService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentService = appointmentService;
    }

    public QueueStatusResponse getQueueStatus(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        LocalDate queryDate = (date != null) ? date : LocalDate.now();

        // 1. Current Token (IN_CONSULTATION)
        Optional<Appointment> inConsultation = appointmentRepository.findFirstByDoctorIdAndAppointmentDateAndStatus(
                doctorId, queryDate, AppointmentStatus.IN_CONSULTATION);
        Integer currentToken = inConsultation.map(Appointment::getTokenNumber).orElse(null);

        // 2. Next Token (first BOOKED)
        Optional<Appointment> nextBooked = appointmentRepository.findFirstByDoctorIdAndAppointmentDateAndStatusOrderByTokenNumberAsc(
                doctorId, queryDate, AppointmentStatus.BOOKED);
        Integer nextToken = nextBooked.map(Appointment::getTokenNumber).orElse(null);

        // 3. Waiting count (total BOOKED)
        List<Appointment> bookedList = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusOrderByTokenNumberAsc(
                doctorId, queryDate, AppointmentStatus.BOOKED);
        int waitingCount = bookedList.size();

        // 4. All appointments for that day
        List<AppointmentResponse> queue = appointmentRepository.findByDoctorIdAndAppointmentDateOrderByTokenNumberAsc(
                doctorId, queryDate).stream()
                .map(appointmentService::mapToResponse)
                .collect(Collectors.toList());

        return new QueueStatusResponse(
                doctor.getId(),
                doctor.getUser().getName(),
                doctor.getSpecialization().getName(),
                queryDate,
                currentToken,
                nextToken,
                waitingCount,
                queue
        );
    }

    @Transactional
    public AppointmentResponse callNextPatient(Long doctorId) {
        LocalDate today = LocalDate.now();

        // 1. Find next waiting patient in BOOKED status
        Optional<Appointment> nextOpt = appointmentRepository.findFirstByDoctorIdAndAppointmentDateAndStatusOrderByTokenNumberAsc(
                doctorId, today, AppointmentStatus.BOOKED);

        if (nextOpt.isEmpty()) {
            throw new BadRequestException("No waiting patients in queue for today.");
        }

        Appointment nextAppointment = nextOpt.get();
        nextAppointment.setStatus(AppointmentStatus.IN_CONSULTATION);
        Appointment saved = appointmentRepository.save(nextAppointment);

        return appointmentService.mapToResponse(saved);
    }
}

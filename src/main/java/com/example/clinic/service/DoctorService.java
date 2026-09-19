package com.example.clinic.service;

import com.example.clinic.dto.DoctorAvailabilityDto;
import com.example.clinic.dto.DoctorDto;
import com.example.clinic.dto.DoctorSlotsResponse;
import com.example.clinic.dto.SlotDto;
import com.example.clinic.entity.Appointment;
import com.example.clinic.entity.AppointmentStatus;
import com.example.clinic.entity.Doctor;
import com.example.clinic.entity.DoctorAvailability;
import com.example.clinic.exception.ResourceNotFoundException;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.DoctorAvailabilityRepository;
import com.example.clinic.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public DoctorService(DoctorRepository doctorRepository,
                         DoctorAvailabilityRepository availabilityRepository,
                         AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<DoctorDto> getAllDoctors(Long specializationId, String search) {
        List<Doctor> doctors;
        if (specializationId != null && specializationId > 0) {
            doctors = doctorRepository.findBySpecializationIdAndIsAvailableTrue(specializationId);
        } else {
            doctors = doctorRepository.findByIsAvailableTrue();
        }

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            doctors = doctors.stream()
                    .filter(d -> d.getUser().getName().toLowerCase().contains(q)
                            || d.getSpecialization().getName().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        return doctors.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    public DoctorDto getDoctorById(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));
        return mapToDto(doctor);
    }

    public DoctorSlotsResponse getDoctorSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + doctorId));

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        Optional<DoctorAvailability> optAvailability = availabilityRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);

        if (optAvailability.isEmpty()) {
            return new DoctorSlotsResponse(
                    doctor.getId(),
                    doctor.getUser().getName(),
                    date,
                    dayOfWeek.name(),
                    "Not Available on " + dayOfWeek.name(),
                    Collections.emptyList()
            );
        }

        DoctorAvailability availability = optAvailability.get();
        LocalTime start = availability.getStartTime();
        LocalTime end = availability.getEndTime();

        String workingHours = start.format(TIME_FORMATTER) + " - " + end.format(TIME_FORMATTER);

        // Fetch already booked slots for this doctor on this date
        List<AppointmentStatus> activeStatuses = Arrays.asList(AppointmentStatus.BOOKED, AppointmentStatus.IN_CONSULTATION);
        List<Appointment> bookedAppointments = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusIn(
                doctorId, date, activeStatuses);

        Set<LocalTime> bookedTimes = bookedAppointments.stream()
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        List<SlotDto> slots = new ArrayList<>();
        LocalTime current = start;
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Generate 30-minute slots
        while (current.isBefore(end)) {
            boolean isBooked = bookedTimes.contains(current);
            boolean isPast = date.isEqual(today) && current.isBefore(now);
            boolean isAvailable = !isBooked && !isPast;

            slots.add(new SlotDto(
                    current,
                    current.format(TIME_FORMATTER),
                    isAvailable
            ));
            current = current.plusMinutes(30);
        }

        return new DoctorSlotsResponse(
                doctor.getId(),
                doctor.getUser().getName(),
                date,
                dayOfWeek.name(),
                workingHours,
                slots
        );
    }

    public DoctorDto mapToDto(Doctor doctor) {
        DoctorDto dto = new DoctorDto();
        dto.setId(doctor.getId());
        dto.setUserId(doctor.getUser().getId());
        dto.setName(doctor.getUser().getName());
        dto.setEmail(doctor.getUser().getEmail());
        dto.setSpecializationId(doctor.getSpecialization().getId());
        dto.setSpecializationName(doctor.getSpecialization().getName());
        dto.setConsultationFee(doctor.getConsultationFee());
        dto.setAvailable(doctor.isAvailable());

        List<DoctorAvailabilityDto> avList = availabilityRepository.findByDoctorId(doctor.getId())
                .stream()
                .map(a -> new DoctorAvailabilityDto(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime()))
                .collect(Collectors.toList());
        dto.setAvailabilities(avList);

        return dto;
    }
}

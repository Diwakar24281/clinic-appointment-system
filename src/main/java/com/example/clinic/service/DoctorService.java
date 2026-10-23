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

import java.math.BigDecimal;
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

    public static final LocalTime LUNCH_START = LocalTime.of(13, 0); // 1:00 PM
    public static final LocalTime LUNCH_END = LocalTime.of(14, 30);   // 2:30 PM

    public List<DoctorDto> getAllDoctors(Long specializationId, String search) {
        return getAllDoctors(specializationId, search, "APPROVED");
    }

    public List<DoctorDto> getAllDoctors(Long specializationId, String search, String status) {
        List<Doctor> doctors = doctorRepository.findAll();

        // Status filtering: ALL, PENDING_APPROVAL, APPROVED (default for public booking)
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            if ("PENDING_APPROVAL".equalsIgnoreCase(status)) {
                doctors = doctors.stream()
                        .filter(d -> "PENDING_APPROVAL".equalsIgnoreCase(d.getApprovalStatus()))
                        .collect(Collectors.toList());
            } else if ("APPROVED".equalsIgnoreCase(status)) {
                doctors = doctors.stream()
                        .filter(d -> "APPROVED".equalsIgnoreCase(d.getApprovalStatus()))
                        .collect(Collectors.toList());
            }
        }

        if (specializationId != null && specializationId > 0) {
            doctors = doctors.stream()
                    .filter(d -> d.getSpecialization() != null && d.getSpecialization().getId().equals(specializationId))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            doctors = doctors.stream()
                    .filter(d -> (d.getUser() != null && d.getUser().getName().toLowerCase().contains(q))
                            || (d.getSpecialization() != null && d.getSpecialization().getName().toLowerCase().contains(q)))
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
        if (start.isBefore(LUNCH_START) && end.isAfter(LUNCH_END)) {
            workingHours += " (Lunch break: 01:00 PM - 02:30 PM)";
        }

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

        // Generate 30-minute slots up to end time, excluding lunch break
        while (current.isBefore(end)) {
            // Prevent any slot from starting in or overlapping lunch break (1:00 PM - 2:30 PM)
            if (current.isBefore(LUNCH_START) && current.plusMinutes(30).isAfter(LUNCH_START)) {
                current = LUNCH_END;
                continue;
            }

            if (!current.isBefore(LUNCH_START) && current.isBefore(LUNCH_END)) {
                current = LUNCH_END;
                continue;
            }


            // Ensure 30-minute slot finishes at or before end time
            if (current.plusMinutes(30).isAfter(end)) {
                break;
            }

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
        dto.setPhone(doctor.getPhone());
        dto.setApprovalStatus(doctor.getApprovalStatus() != null ? doctor.getApprovalStatus() : "APPROVED");

        if (doctor.getSpecialization() != null) {
            dto.setSpecializationId(doctor.getSpecialization().getId());
            dto.setSpecializationName(doctor.getSpecialization().getName());
        } else {
            dto.setSpecializationId(null);
            dto.setSpecializationName("Pending Assignment");
        }

        dto.setConsultationFee(doctor.getConsultationFee() != null ? doctor.getConsultationFee() : BigDecimal.ZERO);
        dto.setAvailable(doctor.isAvailable());

        List<DoctorAvailability> avEntities = availabilityRepository.findByDoctorId(doctor.getId());
        List<DoctorAvailabilityDto> avList = avEntities.stream()
                .map(a -> new DoctorAvailabilityDto(a.getId(), a.getDayOfWeek(), a.getStartTime(), a.getEndTime()))
                .collect(Collectors.toList());
        dto.setAvailabilities(avList);

        // Grounded determination of today's availability status
        if ("PENDING_APPROVAL".equalsIgnoreCase(doctor.getApprovalStatus())) {
            dto.setTodayAvailabilityStatus("Pending Admin Approval");
            dto.setTodayWorkingHours("Awaiting Admin Review");
        } else if ("INACTIVE".equalsIgnoreCase(doctor.getApprovalStatus())) {
            dto.setTodayAvailabilityStatus("Inactive");
            dto.setTodayWorkingHours("Account Inactive");
        } else if (!doctor.isAvailable()) {
            dto.setTodayAvailabilityStatus("On leave");
            dto.setTodayWorkingHours("Unavailable (On Leave)");
        } else if (avEntities.isEmpty()) {
            dto.setTodayAvailabilityStatus("Schedule not configured");
            dto.setTodayWorkingHours("Schedule not configured");
        } else {
            LocalDate today = LocalDate.now();
            DayOfWeek todayDay = today.getDayOfWeek();
            Optional<DoctorAvailability> todayAvOpt = avEntities.stream()
                    .filter(a -> a.getDayOfWeek() == todayDay)
                    .findFirst();

            if (todayAvOpt.isEmpty()) {
                dto.setTodayAvailabilityStatus("Not scheduled today");
                dto.setTodayWorkingHours("Not scheduled on " + todayDay.name().substring(0, 1) + todayDay.name().substring(1).toLowerCase());
            } else {
                DoctorAvailability todayAv = todayAvOpt.get();
                LocalTime start = todayAv.getStartTime();
                LocalTime end = todayAv.getEndTime();
                String hoursStr = start.format(TIME_FORMATTER) + " - " + end.format(TIME_FORMATTER);
                if (start.isBefore(LUNCH_START) && end.isAfter(LUNCH_END)) {
                    hoursStr += " (Lunch: 01:00 PM - 02:30 PM)";
                }
                dto.setTodayWorkingHours(hoursStr);

                // Check active bookings for today
                List<Appointment> todayBookings = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusIn(
                        doctor.getId(), today, Arrays.asList(AppointmentStatus.BOOKED, AppointmentStatus.IN_CONSULTATION));
                Set<LocalTime> bookedTimes = todayBookings.stream()
                        .map(Appointment::getAppointmentTime)
                        .collect(Collectors.toSet());

                int totalSlots = 0;
                int bookedSlots = 0;
                LocalTime curr = start;
                while (curr.isBefore(end)) {
                    if (!curr.isBefore(LUNCH_START) && curr.isBefore(LUNCH_END)) {
                        curr = curr.plusMinutes(30);
                        continue;
                    }
                    if (curr.plusMinutes(30).isAfter(end)) {
                        break;
                    }
                    totalSlots++;
                    if (bookedTimes.contains(curr)) {
                        bookedSlots++;
                    }
                    curr = curr.plusMinutes(30);
                }

                if (totalSlots > 0 && bookedSlots >= totalSlots) {
                    dto.setTodayAvailabilityStatus("Fully booked");
                } else {
                    dto.setTodayAvailabilityStatus("Available today");
                }
            }
        }

        // Summary of working days and times
        if (!avEntities.isEmpty()) {
            String days = avEntities.stream()
                    .map(a -> a.getDayOfWeek().name().substring(0, 3))
                    .distinct()
                    .collect(Collectors.joining(", "));
            dto.setAvailableDaysSummary(days);
            dto.setAvailableDatesAndTimes(days + " (" + avEntities.get(0).getStartTime().format(TIME_FORMATTER) + " - " + avEntities.get(0).getEndTime().format(TIME_FORMATTER) + ")");
        } else {
            dto.setAvailableDaysSummary("None");
            dto.setAvailableDatesAndTimes("Schedule not configured");
        }

        return dto;
    }
}

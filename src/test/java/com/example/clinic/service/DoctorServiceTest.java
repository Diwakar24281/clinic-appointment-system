package com.example.clinic.service;

import com.example.clinic.dto.DoctorDto;
import com.example.clinic.dto.DoctorSlotsResponse;
import com.example.clinic.dto.SlotDto;
import com.example.clinic.entity.Doctor;
import com.example.clinic.entity.DoctorAvailability;
import com.example.clinic.entity.Specialization;
import com.example.clinic.entity.User;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.DoctorAvailabilityRepository;
import com.example.clinic.repository.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorAvailabilityRepository availabilityRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private DoctorService doctorService;

    private Doctor doctor;
    private DoctorAvailability availability;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("Dr. Ramesh Kumar");
        user.setEmail("dr.ramesh@clinic.com");

        Specialization spec = new Specialization();
        spec.setName("Cardiology");

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setUser(user);
        doctor.setSpecialization(spec);
        doctor.setConsultationFee(BigDecimal.valueOf(500.0));
        doctor.setAvailable(true);

        testDate = LocalDate.now().plusDays(2);
        while (testDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            testDate = testDate.plusDays(1);
        }

        availability = new DoctorAvailability();
        availability.setId(10L);
        availability.setDoctor(doctor);
        availability.setDayOfWeek(testDate.getDayOfWeek());
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(22, 0)); // 9:00 AM to 10:00 PM
    }

    @Test
    @DisplayName("Verify generated slots range from 09:00 AM to 09:30 PM (ends at 10:00 PM) and completely exclude 1:00 PM to 2:30 PM")
    void testSlotGenerationScheduleAndLunchBreak() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));
        when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusIn(eq(1L), eq(testDate), any()))
                .thenReturn(Collections.emptyList());

        DoctorSlotsResponse slotsResponse = doctorService.getDoctorSlots(1L, testDate);

        assertNotNull(slotsResponse);
        List<SlotDto> slots = slotsResponse.getSlots();
        assertFalse(slots.isEmpty());

        // First slot must be 09:00 AM
        assertEquals(LocalTime.of(9, 0), slots.get(0).getTime());

        // Last slot must be 09:30 PM (which finishes at 10:00 PM)
        SlotDto lastSlot = slots.get(slots.size() - 1);
        assertEquals(LocalTime.of(21, 30), lastSlot.getTime());

        // Verify none of the lunch break slots exist (1:00 PM, 1:30 PM, 2:00 PM)
        for (SlotDto slot : slots) {
            LocalTime t = slot.getTime();
            boolean inLunch = !t.isBefore(LocalTime.of(13, 0)) && t.isBefore(LocalTime.of(14, 30));
            assertFalse(inLunch, "Slot " + t + " should not be present as it falls in lunch break (1:00 PM - 2:30 PM)");
        }

        // Verify slot before lunch (12:30 PM) is present
        boolean has1230 = slots.stream().anyMatch(s -> s.getTime().equals(LocalTime.of(12, 30)));
        assertTrue(has1230, "12:30 PM slot must be present");

        // Verify slot after lunch (2:30 PM) is present
        boolean has1430 = slots.stream().anyMatch(s -> s.getTime().equals(LocalTime.of(14, 30)));
        assertTrue(has1430, "02:30 PM slot must be present");

        // Verify slot at 10:00 PM is NOT present (as a 30-min consultation would extend past closing)
        boolean has2200 = slots.stream().anyMatch(s -> s.getTime().equals(LocalTime.of(22, 0)));
        assertFalse(has2200, "10:00 PM slot must not be present since consultations take 30 mins");
    }

    @Test
    @DisplayName("Verify DoctorDto includes today availability status and working hours")
    void testDoctorDtoAvailabilityStatus() {
        DoctorAvailability todayAv = new DoctorAvailability();
        todayAv.setId(11L);
        todayAv.setDoctor(doctor);
        todayAv.setDayOfWeek(LocalDate.now().getDayOfWeek());
        todayAv.setStartTime(LocalTime.of(9, 0));
        todayAv.setEndTime(LocalTime.of(22, 0));

        when(availabilityRepository.findByDoctorId(1L)).thenReturn(List.of(todayAv));
        when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusIn(eq(1L), any(LocalDate.class), any()))
                .thenReturn(Collections.emptyList());

        DoctorDto dto = doctorService.mapToDto(doctor);

        assertNotNull(dto);
        assertNotNull(dto.getTodayAvailabilityStatus());
        assertNotNull(dto.getTodayWorkingHours());
        assertNotNull(dto.getAvailableDaysSummary());
    }
}

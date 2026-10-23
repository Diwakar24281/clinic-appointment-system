package com.example.clinic.service;

import com.example.clinic.dto.AppointmentRequest;
import com.example.clinic.dto.AppointmentResponse;
import com.example.clinic.entity.*;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.SlotAlreadyBookedException;
import com.example.clinic.repository.AppointmentRepository;
import com.example.clinic.repository.DoctorAvailabilityRepository;
import com.example.clinic.repository.DoctorRepository;
import com.example.clinic.repository.PatientRepository;
import com.example.clinic.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorAvailabilityRepository availabilityRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private Doctor doctor;
    private Patient patient;
    private DoctorAvailability availability;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        User docUser = new User();
        docUser.setName("Dr. Ramesh");
        docUser.setRole(Role.DOCTOR);

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setUser(docUser);
        doctor.setApprovalStatus("APPROVED");
        Specialization spec = new Specialization();
        spec.setId(1L);
        spec.setName("Cardiology");
        doctor.setSpecialization(spec);
        doctor.setConsultationFee(BigDecimal.valueOf(500.0));
        doctor.setAvailable(true);

        User patientUser = new User();
        patientUser.setName("Jane Doe");
        patientUser.setRole(Role.PATIENT);

        patient = new Patient();
        patient.setId(10L);
        patient.setUser(patientUser);


        // Find next weekday
        testDate = LocalDate.now().plusDays(2);
        while (testDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            testDate = testDate.plusDays(1);
        }

        availability = new DoctorAvailability();
        availability.setId(100L);
        availability.setDoctor(doctor);
        availability.setDayOfWeek(testDate.getDayOfWeek());
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(22, 0)); // 9:00 AM to 10:00 PM
    }

    @Test
    @DisplayName("Successfully book appointment at 9:00 AM (Clinic Opening)")
    void testBookOpeningSlotSuccess() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(9, 0));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(appointmentRepository.countByDoctorIdAndAppointmentDate(1L, testDate)).thenReturn(0);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(50L);
            return a;
        });

        AppointmentResponse res = appointmentService.bookAppointment(req);

        assertNotNull(res);
        assertEquals(LocalTime.of(9, 0), res.getAppointmentTime());
        assertEquals(1, res.getTokenNumber());
    }

    @Test
    @DisplayName("Successfully book appointment at 9:30 PM (Ends at 10:00 PM closing time)")
    void testBookClosingSlotSuccess() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(21, 30)); // 9:30 PM

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(appointmentRepository.countByDoctorIdAndAppointmentDate(1L, testDate)).thenReturn(4);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(51L);
            return a;
        });

        AppointmentResponse res = appointmentService.bookAppointment(req);

        assertNotNull(res);
        assertEquals(LocalTime.of(21, 30), res.getAppointmentTime());
        assertEquals(5, res.getTokenNumber());
    }

    @Test
    @DisplayName("Reject booking during lunch break at 1:00 PM")
    void testRejectLunchBreakSlot1PM() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(13, 0)); // 1:00 PM

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.bookAppointment(req));
        assertTrue(ex.getMessage().contains("lunch break"));
    }

    @Test
    @DisplayName("Reject booking during lunch break at 1:30 PM")
    void testRejectLunchBreakSlot130PM() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(13, 30)); // 1:30 PM

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.bookAppointment(req));
        assertTrue(ex.getMessage().contains("lunch break"));
    }

    @Test
    @DisplayName("Reject booking during lunch break at 2:00 PM")
    void testRejectLunchBreakSlot2PM() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(14, 0)); // 2:00 PM

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.bookAppointment(req));
        assertTrue(ex.getMessage().contains("lunch break"));
    }

    @Test
    @DisplayName("Allow booking at 12:30 PM (Finishes before lunch at 1:00 PM)")
    void testAllowPreLunchSlot1230PM() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(12, 30));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse res = appointmentService.bookAppointment(req);
        assertNotNull(res);
        assertEquals(LocalTime.of(12, 30), res.getAppointmentTime());
    }

    @Test
    @DisplayName("Allow booking at 2:30 PM (Right after lunch concludes)")
    void testAllowPostLunchSlot230PM() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(14, 30));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse res = appointmentService.bookAppointment(req);
        assertNotNull(res);
        assertEquals(LocalTime.of(14, 30), res.getAppointmentTime());
    }

    @Test
    @DisplayName("Reject booking at 10:00 PM because consultation would extend past closing time")
    void testRejectBookingAtOrAfter10PM() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(22, 0)); // 10:00 PM

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> appointmentService.bookAppointment(req));
        assertTrue(ex.getMessage().contains("extends past doctor's scheduled closing time"));
    }

    @Test
    @DisplayName("Reject duplicate booking for slot already reserved")
    void testRejectAlreadyBookedSlot() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(10, 0));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(true);

        assertThrows(SlotAlreadyBookedException.class, () -> appointmentService.bookAppointment(req));
    }

    @Test
    @DisplayName("Handle race condition concurrency with DataIntegrityViolationException")
    void testConcurrentDoubleBookingCatch() {
        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(1L);
        req.setPatientId(10L);
        req.setAppointmentDate(testDate);
        req.setAppointmentTime(LocalTime.of(10, 30));

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(availabilityRepository.findByDoctorIdAndDayOfWeek(1L, testDate.getDayOfWeek()))
                .thenReturn(Optional.of(availability));
        when(appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusIn(any(), any(), any(), any()))
                .thenReturn(false);
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key UK_doctor_slot"));

        assertThrows(SlotAlreadyBookedException.class, () -> appointmentService.bookAppointment(req));
    }
}

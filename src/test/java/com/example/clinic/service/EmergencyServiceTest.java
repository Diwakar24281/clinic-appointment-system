package com.example.clinic.service;

import com.example.clinic.dto.EmergencyAcceptDto;
import com.example.clinic.dto.EmergencyRequestDto;
import com.example.clinic.dto.EmergencyResponseDto;
import com.example.clinic.entity.*;
import com.example.clinic.exception.BadRequestException;
import com.example.clinic.exception.EmergencyAlreadyAcceptedException;
import com.example.clinic.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyServiceTest {

    @Mock
    private EmergencyRequestRepository emergencyRepo;

    @Mock
    private HospitalRepository hospitalRepo;

    @Mock
    private HospitalResponseRepository hospitalResponseRepo;

    @Mock
    private EmergencyAssignmentRepository assignmentRepo;

    @Mock
    private AmbulanceRepository ambulanceRepo;

    @Mock
    private AmbulanceLocationRepository ambulanceLocationRepo;

    @Mock
    private EmergencyStatusHistoryRepository statusHistoryRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private PatientRepository patientRepo;

    @Mock
    private DoctorRepository doctorRepo;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private EmergencyService emergencyService;

    private User user;
    private EmergencyRequest emergency;
    private Hospital hospital;
    private User hospitalUser;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setRole(Role.PATIENT);

        emergency = new EmergencyRequest();
        emergency.setId(100L);
        emergency.setReportedBy(user);
        emergency.setCategory("CHEST_PAIN");
        emergency.setDescription("Severe chest pressure and breathlessness");
        emergency.setCallbackPhone("9876543210");
        emergency.setStatus(EmergencyStatus.ALERTING_HOSPITALS);

        hospitalUser = new User();
        hospitalUser.setId(5L);
        hospitalUser.setName("Apollo Admin");
        hospitalUser.setRole(Role.HOSPITAL);

        hospital = new Hospital();
        hospital.setId(10L);
        hospital.setName("Apollo Specialty Hospital");
        hospital.setUser(hospitalUser);
    }

    @Test
    @DisplayName("Create emergency requires at least GPS or manual address")
    void testCreateEmergencyValidationLocationRequired() {
        EmergencyRequestDto dto = new EmergencyRequestDto();
        dto.setUserId(1L);
        dto.setCategory("TRAUMA");
        dto.setDescription("Road accident");
        dto.setCallbackPhone("9876543210");
        // No latitude, longitude or manual address

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> emergencyService.createEmergency(dto));
        assertTrue(ex.getMessage().contains("GPS location or a manual address is required"));
    }

    @Test
    @DisplayName("Create emergency with GPS succeeds and alerts hospitals")
    void testCreateEmergencySuccess() {
        EmergencyRequestDto dto = new EmergencyRequestDto();
        dto.setUserId(1L);
        dto.setCategory("TRAUMA");
        dto.setDescription("Road accident");
        dto.setCallbackPhone("9876543210");
        dto.setLatitude(12.9716);
        dto.setLongitude(77.5946);

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(patientRepo.findByUserId(1L)).thenReturn(Optional.empty());
        when(emergencyRepo.save(any(EmergencyRequest.class))).thenAnswer(invocation -> {
            EmergencyRequest req = invocation.getArgument(0);
            req.setId(101L);
            return req;
        });

        EmergencyResponseDto res = emergencyService.createEmergency(dto);

        assertNotNull(res);
        assertEquals(101L, res.getId());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/emergencies"), any(EmergencyResponseDto.class));
    }

    @Test
    @DisplayName("Atomic hospital acceptance succeeds for the first responding hospital")
    void testAcceptEmergencyFirstWins() {
        EmergencyAcceptDto acceptDto = new EmergencyAcceptDto();
        acceptDto.setHospitalId(10L);
        acceptDto.setUserId(5L);

        when(hospitalRepo.findById(10L)).thenReturn(Optional.of(hospital));
        when(userRepo.findById(5L)).thenReturn(Optional.of(hospitalUser));
        when(hospitalRepo.findByUserId(5L)).thenReturn(Optional.of(hospital));
        when(hospitalResponseRepo.existsByEmergencyRequestIdAndHospitalIdAndResponseType(100L, 10L, "ACCEPTED"))
                .thenReturn(false);
        // Atomic update returns 1 row updated
        when(emergencyRepo.updateStatusAtomically(100L, EmergencyStatus.ALERTING_HOSPITALS, EmergencyStatus.ACCEPTED))
                .thenReturn(1);
        when(emergencyRepo.findById(100L)).thenReturn(Optional.of(emergency));
        when(assignmentRepo.findByEmergencyRequestId(100L)).thenReturn(Optional.empty());
        when(statusHistoryRepo.findByEmergencyRequestIdOrderByChangedAtAsc(100L)).thenReturn(Collections.emptyList());

        EmergencyResponseDto res = emergencyService.acceptEmergency(100L, acceptDto);

        assertNotNull(res);
        verify(hospitalResponseRepo).save(any(HospitalResponse.class));
        verify(assignmentRepo).save(any(EmergencyAssignment.class));
    }

    @Test
    @DisplayName("Second hospital attempting to accept throws EmergencyAlreadyAcceptedException (Atomic locking)")
    void testAcceptEmergencySecondHospitalConflict() {
        EmergencyAcceptDto acceptDto = new EmergencyAcceptDto();
        acceptDto.setHospitalId(10L);
        acceptDto.setUserId(5L);

        when(hospitalRepo.findById(10L)).thenReturn(Optional.of(hospital));
        when(userRepo.findById(5L)).thenReturn(Optional.of(hospitalUser));
        when(hospitalRepo.findByUserId(5L)).thenReturn(Optional.of(hospital));
        when(hospitalResponseRepo.existsByEmergencyRequestIdAndHospitalIdAndResponseType(100L, 10L, "ACCEPTED"))
                .thenReturn(false);
        // Atomic update returns 0 rows updated because another hospital already flipped the status
        when(emergencyRepo.updateStatusAtomically(100L, EmergencyStatus.ALERTING_HOSPITALS, EmergencyStatus.ACCEPTED))
                .thenReturn(0);

        assertThrows(EmergencyAlreadyAcceptedException.class, () -> emergencyService.acceptEmergency(100L, acceptDto));
    }

    @Test
    @DisplayName("Doctor can acknowledge emergency alert")
    void testDoctorAcknowledgeEmergencyAlert() {
        Doctor doc = new Doctor();
        doc.setId(20L);
        User docUser = new User();
        docUser.setName("Dr. Ramesh");
        doc.setUser(docUser);

        EmergencyAssignment assignment = new EmergencyAssignment(emergency, hospital);
        assignment.setDoctor(doc);
        assignment.setDoctorAcknowledged(false);

        when(assignmentRepo.findByEmergencyRequestId(100L)).thenReturn(Optional.of(assignment));
        when(statusHistoryRepo.findByEmergencyRequestIdOrderByChangedAtAsc(100L)).thenReturn(Collections.emptyList());

        EmergencyResponseDto res = emergencyService.acknowledgeDoctorAlert(100L, 20L);

        assertNotNull(res);
        assertTrue(assignment.isDoctorAcknowledged());
        assertNotNull(assignment.getDoctorAcknowledgedAt());
        verify(assignmentRepo).save(assignment);
    }
}

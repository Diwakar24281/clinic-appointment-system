package com.example.clinic.service;

import com.example.clinic.dto.*;
import com.example.clinic.entity.*;
import com.example.clinic.exception.*;
import com.example.clinic.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core emergency service handling the full lifecycle:
 * creation → hospital alerting → acceptance → assignment → tracking → completion.
 *
 * Uses atomic database updates for first-acceptance-wins concurrency control.
 * Sends WebSocket messages for real-time doctor/hospital/patient notifications.
 */
@Service
public class EmergencyService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyService.class);

    private final EmergencyRequestRepository emergencyRepo;
    private final HospitalRepository hospitalRepo;
    private final HospitalResponseRepository hospitalResponseRepo;
    private final EmergencyAssignmentRepository assignmentRepo;
    private final AmbulanceRepository ambulanceRepo;
    private final AmbulanceLocationRepository ambulanceLocationRepo;
    private final EmergencyStatusHistoryRepository statusHistoryRepo;
    private final UserRepository userRepo;
    private final PatientRepository patientRepo;
    private final DoctorRepository doctorRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public EmergencyService(EmergencyRequestRepository emergencyRepo,
                            HospitalRepository hospitalRepo,
                            HospitalResponseRepository hospitalResponseRepo,
                            EmergencyAssignmentRepository assignmentRepo,
                            AmbulanceRepository ambulanceRepo,
                            AmbulanceLocationRepository ambulanceLocationRepo,
                            EmergencyStatusHistoryRepository statusHistoryRepo,
                            UserRepository userRepo,
                            PatientRepository patientRepo,
                            DoctorRepository doctorRepo,
                            SimpMessagingTemplate messagingTemplate) {
        this.emergencyRepo = emergencyRepo;
        this.hospitalRepo = hospitalRepo;
        this.hospitalResponseRepo = hospitalResponseRepo;
        this.assignmentRepo = assignmentRepo;
        this.ambulanceRepo = ambulanceRepo;
        this.ambulanceLocationRepo = ambulanceLocationRepo;
        this.statusHistoryRepo = statusHistoryRepo;
        this.userRepo = userRepo;
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
        this.messagingTemplate = messagingTemplate;
    }

    // ==================== EMERGENCY CREATION ====================

    /**
     * Creates a new emergency request from an authenticated user.
     * Validates user identity, saves the emergency, and alerts hospitals.
     */
    @Transactional
    public EmergencyResponseDto createEmergency(EmergencyRequestDto dto) {
        // 1. Validate authenticated user
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // 2. Validate location: at least GPS or manual address must be provided
        if ((dto.getLatitude() == null || dto.getLongitude() == null)
                && (dto.getManualAddress() == null || dto.getManualAddress().isBlank())) {
            throw new BadRequestException("Either GPS location or a manual address is required.");
        }

        // 3. Build and save the emergency request
        EmergencyRequest emergency = new EmergencyRequest();
        emergency.setReportedBy(user);
        emergency.setCategory(dto.getCategory());
        emergency.setDescription(dto.getDescription());
        emergency.setCallbackPhone(dto.getCallbackPhone());
        emergency.setLatitude(dto.getLatitude());
        emergency.setLongitude(dto.getLongitude());
        emergency.setLocationAccuracy(dto.getLocationAccuracy());
        emergency.setManualAddress(dto.getManualAddress());
        emergency.setLandmark(dto.getLandmark());
        emergency.setArea(dto.getArea());
        emergency.setStatus(EmergencyStatus.CREATED);

        // Link patient profile if user is a patient
        if (user.getRole() == Role.PATIENT) {
            patientRepo.findByUserId(user.getId()).ifPresent(emergency::setPatient);
        }

        emergency = emergencyRepo.save(emergency);

        // 4. Record status history
        recordStatusChange(emergency, null, EmergencyStatus.CREATED, user.getId(), "Emergency reported");

        // 5. Alert hospitals and update status
        emergency.setStatus(EmergencyStatus.ALERTING_HOSPITALS);
        emergency = emergencyRepo.save(emergency);
        recordStatusChange(emergency, EmergencyStatus.CREATED, EmergencyStatus.ALERTING_HOSPITALS,
                user.getId(), "Alerting nearby hospitals");

        // 6. Notify hospitals via WebSocket
        EmergencyResponseDto responseDto = buildResponseDto(emergency);
        messagingTemplate.convertAndSend("/topic/emergencies", responseDto);

        log.info("Emergency #{} created by user #{}, alerting hospitals", emergency.getId(), user.getId());
        return responseDto;
    }

    // ==================== HOSPITAL ACCEPTANCE (First-Wins) ====================

    /**
     * Atomically accepts an emergency for a hospital.
     * Uses UPDATE ... WHERE status = ALERTING_HOSPITALS to ensure only one hospital wins.
     */
    @Transactional
    public EmergencyResponseDto acceptEmergency(Long emergencyId, EmergencyAcceptDto dto) {
        // 1. Validate hospital
        Hospital hospital = hospitalRepo.findById(dto.getHospitalId())
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found."));

        // Validate that the user belongs to this hospital
        User hospitalUser = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        Hospital userHospital = hospitalRepo.findByUserId(hospitalUser.getId())
                .orElseThrow(() -> new BadRequestException("User is not associated with any hospital."));
        if (!userHospital.getId().equals(hospital.getId())) {
            throw new BadRequestException("User does not belong to the specified hospital.");
        }

        // 2. Check if this hospital already responded
        if (hospitalResponseRepo.existsByEmergencyRequestIdAndHospitalIdAndResponseType(
                emergencyId, hospital.getId(), "ACCEPTED")) {
            throw new BadRequestException("Hospital has already accepted this emergency.");
        }

        // 3. Atomic status update: only succeeds if status is still ALERTING_HOSPITALS
        int updatedRows = emergencyRepo.updateStatusAtomically(
                emergencyId, EmergencyStatus.ALERTING_HOSPITALS, EmergencyStatus.ACCEPTED);

        if (updatedRows == 0) {
            // Another hospital already accepted — return conflict
            throw new EmergencyAlreadyAcceptedException(
                    "This emergency has already been accepted by another hospital.");
        }

        // 4. Record hospital acceptance
        EmergencyRequest emergency = emergencyRepo.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency not found."));

        HospitalResponse acceptance = new HospitalResponse(emergency, hospital, "ACCEPTED");
        hospitalResponseRepo.save(acceptance);

        // 5. Create assignment record
        EmergencyAssignment assignment = new EmergencyAssignment(emergency, hospital);
        assignmentRepo.save(assignment);

        // 6. Record status history
        recordStatusChange(emergency, EmergencyStatus.ALERTING_HOSPITALS, EmergencyStatus.ACCEPTED,
                hospitalUser.getId(), "Accepted by " + hospital.getName());

        // 7. Notify patient and other hospitals via WebSocket
        EmergencyResponseDto responseDto = buildResponseDto(emergency);
        messagingTemplate.convertAndSend("/topic/emergency/" + emergencyId, responseDto);
        messagingTemplate.convertAndSend("/topic/emergencies", responseDto);

        log.info("Emergency #{} accepted by hospital {} ({})", emergencyId, hospital.getId(), hospital.getName());
        return responseDto;
    }

    // ==================== HOSPITAL REJECTION ====================

    @Transactional
    public ApiResponse rejectEmergency(Long emergencyId, Long hospitalId, Long userId, String reason) {
        Hospital hospital = hospitalRepo.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital not found."));

        EmergencyRequest emergency = emergencyRepo.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency not found."));

        HospitalResponse rejection = new HospitalResponse(emergency, hospital, "REJECTED");
        rejection.setReason(reason);
        hospitalResponseRepo.save(rejection);

        log.info("Emergency #{} rejected by hospital {} (reason: {})", emergencyId, hospitalId, reason);
        return new ApiResponse(true, "Emergency rejected by hospital.");
    }

    // ==================== DOCTOR & AMBULANCE ASSIGNMENT ====================

    /**
     * Assigns a doctor and ambulance to an accepted emergency.
     * Validates that both are available and belong to the accepting hospital.
     */
    @Transactional
    public EmergencyResponseDto assignDoctorAndAmbulance(Long emergencyId, EmergencyAssignDto dto) {
        EmergencyRequest emergency = emergencyRepo.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency not found."));

        EmergencyAssignment assignment = assignmentRepo.findByEmergencyRequestId(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("No assignment found. Hospital must accept first."));

        // Validate hospital ownership
        if (!assignment.getHospital().getId().equals(dto.getHospitalId())) {
            throw new BadRequestException("Only the accepting hospital can assign resources.");
        }

        // Assign doctor if provided
        if (dto.getDoctorId() != null) {
            Doctor doctor = doctorRepo.findById(dto.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found."));
            if (!doctor.isAvailable()) {
                throw new BadRequestException("Selected doctor is not currently available.");
            }
            assignment.setDoctor(doctor);
            assignment.setDoctorAssignedAt(LocalDateTime.now());

            // Update emergency status
            emergency.setStatus(EmergencyStatus.DOCTOR_ASSIGNED);
            emergencyRepo.save(emergency);
            recordStatusChange(emergency, EmergencyStatus.ACCEPTED, EmergencyStatus.DOCTOR_ASSIGNED,
                    dto.getUserId(), "Doctor assigned: " + doctor.getUser().getName());

            // Notify doctor via WebSocket
            messagingTemplate.convertAndSend("/topic/doctor/" + doctor.getId(), buildResponseDto(emergency));
            log.info("Doctor #{} assigned to emergency #{}", doctor.getId(), emergencyId);
        }

        // Assign ambulance if provided
        if (dto.getAmbulanceId() != null) {
            Ambulance ambulance = ambulanceRepo.findById(dto.getAmbulanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ambulance not found."));
            if (!ambulance.isAvailable()) {
                throw new BadRequestException("Selected ambulance is not currently available.");
            }
            if (!ambulance.getHospital().getId().equals(dto.getHospitalId())) {
                throw new BadRequestException("Ambulance does not belong to this hospital.");
            }

            ambulance.setAvailable(false);
            ambulanceRepo.save(ambulance);

            assignment.setAmbulance(ambulance);
            assignment.setAmbulanceDispatchedAt(LocalDateTime.now());

            EmergencyStatus prevStatus = emergency.getStatus();
            emergency.setStatus(EmergencyStatus.AMBULANCE_DISPATCHED);
            emergencyRepo.save(emergency);
            recordStatusChange(emergency, prevStatus, EmergencyStatus.AMBULANCE_DISPATCHED,
                    dto.getUserId(), "Ambulance dispatched: " + ambulance.getVehicleNumber());
            log.info("Ambulance {} dispatched for emergency #{}", ambulance.getVehicleNumber(), emergencyId);
        }

        if (dto.getEstimatedArrivalMinutes() != null) {
            assignment.setEstimatedArrivalMinutes(dto.getEstimatedArrivalMinutes());
        }

        assignmentRepo.save(assignment);

        // Notify patient via WebSocket
        EmergencyResponseDto responseDto = buildResponseDto(emergency);
        messagingTemplate.convertAndSend("/topic/emergency/" + emergencyId, responseDto);
        return responseDto;
    }

    // ==================== DOCTOR ACKNOWLEDGEMENT ====================

    @Transactional
    public EmergencyResponseDto acknowledgeDoctorAlert(Long emergencyId, Long doctorId) {
        EmergencyAssignment assignment = assignmentRepo.findByEmergencyRequestId(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("No assignment found for this emergency."));

        if (assignment.getDoctor() == null || !assignment.getDoctor().getId().equals(doctorId)) {
            throw new BadRequestException("Doctor is not assigned to this emergency.");
        }

        assignment.setDoctorAcknowledged(true);
        assignment.setDoctorAcknowledgedAt(LocalDateTime.now());
        assignmentRepo.save(assignment);

        log.info("Doctor #{} acknowledged emergency #{}", doctorId, emergencyId);
        return buildResponseDto(assignment.getEmergencyRequest());
    }

    // ==================== STATUS UPDATES ====================

    @Transactional
    public EmergencyResponseDto updateStatus(Long emergencyId, String newStatusStr, Long userId) {
        EmergencyRequest emergency = emergencyRepo.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency not found."));

        EmergencyStatus newStatus;
        try {
            newStatus = EmergencyStatus.valueOf(newStatusStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid emergency status: " + newStatusStr);
        }

        EmergencyStatus oldStatus = emergency.getStatus();
        emergency.setStatus(newStatus);
        emergencyRepo.save(emergency);

        recordStatusChange(emergency, oldStatus, newStatus, userId, "Status updated to " + newStatus);

        // If completed, release ambulance
        if (newStatus == EmergencyStatus.COMPLETED || newStatus == EmergencyStatus.CANCELLED) {
            assignmentRepo.findByEmergencyRequestId(emergencyId).ifPresent(assignment -> {
                if (assignment.getAmbulance() != null) {
                    Ambulance ambulance = assignment.getAmbulance();
                    ambulance.setAvailable(true);
                    ambulanceRepo.save(ambulance);
                }
            });
        }

        EmergencyResponseDto responseDto = buildResponseDto(emergency);
        messagingTemplate.convertAndSend("/topic/emergency/" + emergencyId, responseDto);
        messagingTemplate.convertAndSend("/topic/emergencies", responseDto);
        return responseDto;
    }

    // ==================== AMBULANCE LOCATION UPDATES ====================

    @Transactional
    public ApiResponse updateAmbulanceLocation(Long emergencyId, AmbulanceLocationDto dto) {
        EmergencyRequest emergency = emergencyRepo.findById(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency not found."));

        EmergencyAssignment assignment = assignmentRepo.findByEmergencyRequestId(emergencyId)
                .orElseThrow(() -> new ResourceNotFoundException("No assignment found."));

        if (assignment.getAmbulance() == null) {
            throw new BadRequestException("No ambulance assigned to this emergency.");
        }

        // Save location update
        AmbulanceLocation location = new AmbulanceLocation();
        location.setEmergencyRequest(emergency);
        location.setAmbulance(assignment.getAmbulance());
        location.setLatitude(dto.getLatitude());
        location.setLongitude(dto.getLongitude());
        location.setSpeedKmph(dto.getSpeedKmph());
        location.setSimulated(dto.isSimulated());
        ambulanceLocationRepo.save(location);

        // Update ambulance current position
        Ambulance ambulance = assignment.getAmbulance();
        ambulance.setCurrentLatitude(dto.getLatitude());
        ambulance.setCurrentLongitude(dto.getLongitude());
        ambulance.setLocationUpdatedAt(LocalDateTime.now());
        ambulanceRepo.save(ambulance);

        // Notify patient via WebSocket
        EmergencyResponseDto responseDto = buildResponseDto(emergency);
        messagingTemplate.convertAndSend("/topic/emergency/" + emergencyId, responseDto);

        return new ApiResponse(true, "Ambulance location updated.");
    }

    // ==================== RETRIEVAL METHODS ====================

    public EmergencyResponseDto getEmergencyById(Long id) {
        EmergencyRequest emergency = emergencyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency not found."));
        return buildResponseDto(emergency);
    }

    public List<EmergencyResponseDto> getEmergenciesByUser(Long userId) {
        return emergencyRepo.findByReportedByIdOrderByCreatedAtDesc(userId)
                .stream().map(this::buildResponseDto).collect(Collectors.toList());
    }

    public List<EmergencyResponseDto> getActiveEmergencies() {
        List<EmergencyStatus> activeStatuses = Arrays.asList(
                EmergencyStatus.CREATED, EmergencyStatus.ALERTING_HOSPITALS,
                EmergencyStatus.ACCEPTED, EmergencyStatus.DOCTOR_ASSIGNED,
                EmergencyStatus.AMBULANCE_DISPATCHED, EmergencyStatus.EN_ROUTE);
        return emergencyRepo.findByStatusInOrderByCreatedAtDesc(activeStatuses)
                .stream().map(this::buildResponseDto).collect(Collectors.toList());
    }

    public List<EmergencyResponseDto> getEmergenciesForDoctor(Long doctorId) {
        return assignmentRepo.findByDoctorId(doctorId).stream()
                .map(a -> buildResponseDto(a.getEmergencyRequest()))
                .collect(Collectors.toList());
    }

    public List<EmergencyResponseDto> getUnacknowledgedForDoctor(Long doctorId) {
        return assignmentRepo.findByDoctorIdAndDoctorAcknowledgedFalse(doctorId).stream()
                .map(a -> buildResponseDto(a.getEmergencyRequest()))
                .collect(Collectors.toList());
    }

    public EmergencyResponseDto getActiveEmergencyForUser(Long userId) {
        List<EmergencyStatus> terminalStatuses = Arrays.asList(
                EmergencyStatus.COMPLETED, EmergencyStatus.CANCELLED, EmergencyStatus.TIMEOUT);
        return emergencyRepo.findTopByReportedByIdAndStatusNotInOrderByCreatedAtDesc(userId, terminalStatuses)
                .map(this::buildResponseDto)
                .orElse(null);
    }

    // ==================== HOSPITAL-SPECIFIC RETRIEVAL ====================

    public List<EmergencyResponseDto> getEmergenciesForHospital(Long hospitalId) {
        // Return active emergencies that this hospital can act on
        List<EmergencyStatus> activeStatuses = Arrays.asList(
                EmergencyStatus.ALERTING_HOSPITALS, EmergencyStatus.ACCEPTED,
                EmergencyStatus.DOCTOR_ASSIGNED, EmergencyStatus.AMBULANCE_DISPATCHED,
                EmergencyStatus.EN_ROUTE);
        List<EmergencyResponseDto> result = new ArrayList<>();

        // Emergencies seeking hospitals
        emergencyRepo.findByStatusInOrderByCreatedAtDesc(
                Arrays.asList(EmergencyStatus.ALERTING_HOSPITALS)
        ).forEach(e -> result.add(buildResponseDto(e)));

        // Emergencies assigned to this hospital
        assignmentRepo.findByHospitalId(hospitalId).stream()
                .filter(a -> activeStatuses.contains(a.getEmergencyRequest().getStatus()))
                .forEach(a -> {
                    EmergencyResponseDto dto = buildResponseDto(a.getEmergencyRequest());
                    if (!result.stream().anyMatch(r -> r.getId().equals(dto.getId()))) {
                        result.add(dto);
                    }
                });

        return result;
    }

    // ==================== HELPER METHODS ====================

    private void recordStatusChange(EmergencyRequest emergency, EmergencyStatus from,
                                     EmergencyStatus to, Long userId, String notes) {
        EmergencyStatusHistory history = new EmergencyStatusHistory(emergency, from, to, userId, notes);
        statusHistoryRepo.save(history);
    }

    /**
     * Builds a comprehensive response DTO from an EmergencyRequest entity,
     * enriching it with assignment, tracking, and history data.
     */
    private EmergencyResponseDto buildResponseDto(EmergencyRequest emergency) {
        EmergencyResponseDto dto = new EmergencyResponseDto();
        dto.setId(emergency.getId());
        dto.setReportedByName(emergency.getReportedBy().getName());
        dto.setReportedByUserId(emergency.getReportedBy().getId());
        dto.setCategory(emergency.getCategory());
        dto.setDescription(emergency.getDescription());
        dto.setCallbackPhone(emergency.getCallbackPhone());
        dto.setLatitude(emergency.getLatitude());
        dto.setLongitude(emergency.getLongitude());
        dto.setLocationAccuracy(emergency.getLocationAccuracy());
        dto.setManualAddress(emergency.getManualAddress());
        dto.setLandmark(emergency.getLandmark());
        dto.setArea(emergency.getArea());
        dto.setStatus(emergency.getStatus());
        dto.setCreatedAt(emergency.getCreatedAt());
        dto.setUpdatedAt(emergency.getUpdatedAt());

        // Enrich with assignment data
        assignmentRepo.findByEmergencyRequestId(emergency.getId()).ifPresent(assignment -> {
            dto.setAssignedHospitalName(assignment.getHospital().getName());
            dto.setAssignedHospitalId(assignment.getHospital().getId());
            dto.setAcceptedAt(assignment.getAcceptedAt());
            dto.setDoctorAcknowledged(assignment.isDoctorAcknowledged());
            dto.setEstimatedArrivalMinutes(assignment.getEstimatedArrivalMinutes());
            dto.setDoctorAssignedAt(assignment.getDoctorAssignedAt());
            dto.setAmbulanceDispatchedAt(assignment.getAmbulanceDispatchedAt());

            if (assignment.getDoctor() != null) {
                dto.setAssignedDoctorName(assignment.getDoctor().getUser().getName());
                dto.setAssignedDoctorId(assignment.getDoctor().getId());
            }
            if (assignment.getAmbulance() != null) {
                dto.setAssignedAmbulanceNumber(assignment.getAmbulance().getVehicleNumber());
                dto.setAssignedAmbulanceId(assignment.getAmbulance().getId());
            }
        });

        // Enrich with latest ambulance location
        ambulanceLocationRepo.findTopByEmergencyRequestIdOrderByRecordedAtDesc(emergency.getId())
                .ifPresent(loc -> {
                    dto.setAmbulanceLatitude(loc.getLatitude());
                    dto.setAmbulanceLongitude(loc.getLongitude());
                    dto.setAmbulanceLocationUpdatedAt(loc.getRecordedAt());
                    dto.setAmbulanceLocationSimulated(loc.isSimulated());
                });

        // Enrich with status history
        List<EmergencyResponseDto.StatusHistoryItem> history =
                statusHistoryRepo.findByEmergencyRequestIdOrderByChangedAtAsc(emergency.getId())
                        .stream()
                        .map(h -> new EmergencyResponseDto.StatusHistoryItem(
                                h.getFromStatus() != null ? h.getFromStatus().name() : null,
                                h.getToStatus().name(),
                                h.getNotes(),
                                h.getChangedAt()))
                        .collect(Collectors.toList());
        dto.setStatusHistory(history);

        return dto;
    }
}

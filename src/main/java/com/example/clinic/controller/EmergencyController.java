package com.example.clinic.controller;

import com.example.clinic.dto.*;
import com.example.clinic.service.EmergencyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergencies")
@CrossOrigin(origins = "*")
public class EmergencyController {

    private final EmergencyService emergencyService;

    public EmergencyController(EmergencyService emergencyService) {
        this.emergencyService = emergencyService;
    }

    /**
     * Report a new emergency.
     */
    @PostMapping
    public ResponseEntity<EmergencyResponseDto> createEmergency(@Valid @RequestBody EmergencyRequestDto dto) {
        EmergencyResponseDto response = emergencyService.createEmergency(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get emergency by ID with full details, assignment and tracking.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EmergencyResponseDto> getEmergencyById(@PathVariable Long id) {
        return ResponseEntity.ok(emergencyService.getEmergencyById(id));
    }

    /**
     * Get all active emergencies.
     */
    @GetMapping("/active")
    public ResponseEntity<List<EmergencyResponseDto>> getActiveEmergencies() {
        return ResponseEntity.ok(emergencyService.getActiveEmergencies());
    }

    /**
     * Get all emergencies reported by a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<EmergencyResponseDto>> getEmergenciesByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(emergencyService.getEmergenciesByUser(userId));
    }

    /**
     * Get current active emergency for a user (if any).
     */
    @GetMapping("/active/user/{userId}")
    public ResponseEntity<EmergencyResponseDto> getActiveEmergencyForUser(@PathVariable Long userId) {
        EmergencyResponseDto active = emergencyService.getActiveEmergencyForUser(userId);
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    /**
     * Hospital accepts emergency (atomic first-wins).
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<EmergencyResponseDto> acceptEmergency(
            @PathVariable Long id,
            @RequestBody EmergencyAcceptDto dto) {
        return ResponseEntity.ok(emergencyService.acceptEmergency(id, dto));
    }

    /**
     * Hospital rejects emergency.
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse> rejectEmergency(
            @PathVariable Long id,
            @RequestParam Long hospitalId,
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "Declined by hospital") String reason) {
        return ResponseEntity.ok(emergencyService.rejectEmergency(id, hospitalId, userId, reason));
    }

    /**
     * Assign doctor and/or ambulance to emergency.
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<EmergencyResponseDto> assignDoctorAndAmbulance(
            @PathVariable Long id,
            @RequestBody EmergencyAssignDto dto) {
        return ResponseEntity.ok(emergencyService.assignDoctorAndAmbulance(id, dto));
    }

    /**
     * Doctor acknowledges emergency alert.
     */
    @PostMapping("/{id}/doctor-ack")
    public ResponseEntity<EmergencyResponseDto> acknowledgeDoctorAlert(
            @PathVariable Long id,
            @RequestParam Long doctorId) {
        return ResponseEntity.ok(emergencyService.acknowledgeDoctorAlert(id, doctorId));
    }

    /**
     * Update emergency status.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<EmergencyResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam Long userId) {
        return ResponseEntity.ok(emergencyService.updateStatus(id, status, userId));
    }

    /**
     * Post ambulance location ping.
     */
    @PostMapping("/{id}/ambulance-location")
    public ResponseEntity<ApiResponse> updateAmbulanceLocation(
            @PathVariable Long id,
            @RequestBody AmbulanceLocationDto dto) {
        return ResponseEntity.ok(emergencyService.updateAmbulanceLocation(id, dto));
    }

    /**
     * Get emergencies for doctor.
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<EmergencyResponseDto>> getEmergenciesForDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(emergencyService.getEmergenciesForDoctor(doctorId));
    }

    /**
     * Get unacknowledged emergencies for doctor alert popups.
     */
    @GetMapping("/doctor/{doctorId}/unacknowledged")
    public ResponseEntity<List<EmergencyResponseDto>> getUnacknowledgedForDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(emergencyService.getUnacknowledgedForDoctor(doctorId));
    }

    /**
     * Get emergencies relevant to a specific hospital.
     */
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<EmergencyResponseDto>> getEmergenciesForHospital(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(emergencyService.getEmergenciesForHospital(hospitalId));
    }
}

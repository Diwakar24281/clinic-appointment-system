package com.example.clinic.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Records a hospital's acceptance or rejection response for an emergency.
 * Multiple hospitals may be alerted, but only one can successfully accept.
 */
@Entity
@Table(name = "hospital_responses", indexes = {
    @Index(name = "idx_hr_emergency", columnList = "emergency_id"),
    @Index(name = "idx_hr_hospital", columnList = "hospital_id")
})
public class HospitalResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "emergency_id", referencedColumnName = "id")
    private EmergencyRequest emergencyRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hospital_id", referencedColumnName = "id")
    private Hospital hospital;

    @Column(name = "response_type", nullable = false, length = 20)
    private String responseType; // "ACCEPTED", "REJECTED", "TIMEOUT"

    @Column(length = 300)
    private String reason; // Reason for rejection, if applicable

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public HospitalResponse() {}

    public HospitalResponse(EmergencyRequest emergencyRequest, Hospital hospital, String responseType) {
        this.emergencyRequest = emergencyRequest;
        this.hospital = hospital;
        this.responseType = responseType;
        this.respondedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmergencyRequest getEmergencyRequest() { return emergencyRequest; }
    public void setEmergencyRequest(EmergencyRequest emergencyRequest) { this.emergencyRequest = emergencyRequest; }

    public Hospital getHospital() { return hospital; }
    public void setHospital(Hospital hospital) { this.hospital = hospital; }

    public String getResponseType() { return responseType; }
    public void setResponseType(String responseType) { this.responseType = responseType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }
}

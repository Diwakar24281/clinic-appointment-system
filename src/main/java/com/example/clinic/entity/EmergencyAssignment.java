package com.example.clinic.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Links an accepted emergency to the assigned hospital, doctor, and ambulance.
 * Created when a hospital accepts an emergency. Doctor and ambulance assigned later.
 */
@Entity
@Table(name = "emergency_assignments", indexes = {
    @Index(name = "idx_ea_emergency", columnList = "emergency_id"),
    @Index(name = "idx_ea_doctor", columnList = "doctor_id")
})
public class EmergencyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "emergency_id", referencedColumnName = "id", unique = true)
    private EmergencyRequest emergencyRequest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hospital_id", referencedColumnName = "id")
    private Hospital hospital;

    @ManyToOne
    @JoinColumn(name = "doctor_id", referencedColumnName = "id")
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "ambulance_id", referencedColumnName = "id")
    private Ambulance ambulance;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "doctor_assigned_at")
    private LocalDateTime doctorAssignedAt;

    @Column(name = "ambulance_dispatched_at")
    private LocalDateTime ambulanceDispatchedAt;

    @Column(name = "doctor_acknowledged", nullable = false)
    private boolean doctorAcknowledged = false;

    @Column(name = "doctor_acknowledged_at")
    private LocalDateTime doctorAcknowledgedAt;

    @Column(name = "estimated_arrival_minutes")
    private Integer estimatedArrivalMinutes;

    public EmergencyAssignment() {}

    public EmergencyAssignment(EmergencyRequest emergencyRequest, Hospital hospital) {
        this.emergencyRequest = emergencyRequest;
        this.hospital = hospital;
        this.acceptedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmergencyRequest getEmergencyRequest() { return emergencyRequest; }
    public void setEmergencyRequest(EmergencyRequest emergencyRequest) { this.emergencyRequest = emergencyRequest; }

    public Hospital getHospital() { return hospital; }
    public void setHospital(Hospital hospital) { this.hospital = hospital; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public Ambulance getAmbulance() { return ambulance; }
    public void setAmbulance(Ambulance ambulance) { this.ambulance = ambulance; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getDoctorAssignedAt() { return doctorAssignedAt; }
    public void setDoctorAssignedAt(LocalDateTime doctorAssignedAt) { this.doctorAssignedAt = doctorAssignedAt; }

    public LocalDateTime getAmbulanceDispatchedAt() { return ambulanceDispatchedAt; }
    public void setAmbulanceDispatchedAt(LocalDateTime ambulanceDispatchedAt) { this.ambulanceDispatchedAt = ambulanceDispatchedAt; }

    public boolean isDoctorAcknowledged() { return doctorAcknowledged; }
    public void setDoctorAcknowledged(boolean doctorAcknowledged) { this.doctorAcknowledged = doctorAcknowledged; }

    public LocalDateTime getDoctorAcknowledgedAt() { return doctorAcknowledgedAt; }
    public void setDoctorAcknowledgedAt(LocalDateTime doctorAcknowledgedAt) { this.doctorAcknowledgedAt = doctorAcknowledgedAt; }

    public Integer getEstimatedArrivalMinutes() { return estimatedArrivalMinutes; }
    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) { this.estimatedArrivalMinutes = estimatedArrivalMinutes; }
}

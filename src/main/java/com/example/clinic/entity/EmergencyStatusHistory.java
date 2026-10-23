package com.example.clinic.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Audit trail for every emergency status change.
 * Provides accountability and allows reviewing the full emergency timeline.
 */
@Entity
@Table(name = "emergency_status_history", indexes = {
    @Index(name = "idx_esh_emergency", columnList = "emergency_id")
})
public class EmergencyStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "emergency_id", referencedColumnName = "id")
    private EmergencyRequest emergencyRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private EmergencyStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private EmergencyStatus toStatus;

    @Column(name = "changed_by")
    private Long changedByUserId;

    @Column(length = 300)
    private String notes;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    public EmergencyStatusHistory() {}

    public EmergencyStatusHistory(EmergencyRequest emergencyRequest, EmergencyStatus fromStatus,
                                   EmergencyStatus toStatus, Long changedByUserId, String notes) {
        this.emergencyRequest = emergencyRequest;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedByUserId = changedByUserId;
        this.notes = notes;
        this.changedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public EmergencyRequest getEmergencyRequest() { return emergencyRequest; }
    public void setEmergencyRequest(EmergencyRequest emergencyRequest) { this.emergencyRequest = emergencyRequest; }

    public EmergencyStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(EmergencyStatus fromStatus) { this.fromStatus = fromStatus; }

    public EmergencyStatus getToStatus() { return toStatus; }
    public void setToStatus(EmergencyStatus toStatus) { this.toStatus = toStatus; }

    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}

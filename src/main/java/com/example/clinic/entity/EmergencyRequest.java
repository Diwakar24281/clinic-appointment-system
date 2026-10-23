package com.example.clinic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

/**
 * Core emergency request entity. Created when a patient reports an emergency.
 * Contains symptoms, location, status, and links to the reporting user.
 */
@Entity
@Table(name = "emergency_requests", indexes = {
    @Index(name = "idx_emergency_status", columnList = "status"),
    @Index(name = "idx_emergency_user", columnList = "user_id"),
    @Index(name = "idx_emergency_created", columnList = "created_at")
})
public class EmergencyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user who reported the emergency (must be authenticated)
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User reportedBy;

    // Optional link to patient profile (if reporter is a patient)
    @ManyToOne
    @JoinColumn(name = "patient_id", referencedColumnName = "id")
    private Patient patient;

    @Column(length = 100)
    private String category; // e.g., "Cardiac", "Accident", "Breathing", "Other"

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Emergency description is required")
    private String description;

    @Column(name = "callback_phone", length = 20)
    @NotBlank(message = "Callback phone number is required")
    private String callbackPhone;

    // GPS location from browser Geolocation API
    @Column(precision = 9)
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @Column(precision = 9)
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    @Column(name = "location_accuracy")
    private Double locationAccuracy; // in meters

    // Manual address fallback
    @Column(name = "manual_address", length = 500)
    private String manualAddress;

    @Column(length = 200)
    private String landmark;

    @Column(length = 200)
    private String area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmergencyStatus status = EmergencyStatus.CREATED;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Version for optimistic locking on concurrent acceptance
    @Version
    private Long version;

    public EmergencyRequest() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCallbackPhone() { return callbackPhone; }
    public void setCallbackPhone(String callbackPhone) { this.callbackPhone = callbackPhone; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLocationAccuracy() { return locationAccuracy; }
    public void setLocationAccuracy(Double locationAccuracy) { this.locationAccuracy = locationAccuracy; }

    public String getManualAddress() { return manualAddress; }
    public void setManualAddress(String manualAddress) { this.manualAddress = manualAddress; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public EmergencyStatus getStatus() { return status; }
    public void setStatus(EmergencyStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

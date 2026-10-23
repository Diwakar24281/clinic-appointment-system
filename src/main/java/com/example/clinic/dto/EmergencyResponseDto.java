package com.example.clinic.dto;

import com.example.clinic.entity.EmergencyStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for returning emergency details to the frontend.
 * Combines data from EmergencyRequest, EmergencyAssignment, and related entities.
 */
public class EmergencyResponseDto {

    private Long id;
    private String reportedByName;
    private Long reportedByUserId;
    private String category;
    private String description;
    private String callbackPhone;
    private Double latitude;
    private Double longitude;
    private Double locationAccuracy;
    private String manualAddress;
    private String landmark;
    private String area;
    private EmergencyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Assignment details (populated when assigned)
    private String assignedHospitalName;
    private Long assignedHospitalId;
    private String assignedDoctorName;
    private Long assignedDoctorId;
    private String assignedAmbulanceNumber;
    private Long assignedAmbulanceId;
    private boolean doctorAcknowledged;
    private Integer estimatedArrivalMinutes;
    private LocalDateTime acceptedAt;
    private LocalDateTime doctorAssignedAt;
    private LocalDateTime ambulanceDispatchedAt;

    // Tracking data
    private Double ambulanceLatitude;
    private Double ambulanceLongitude;
    private LocalDateTime ambulanceLocationUpdatedAt;
    private boolean ambulanceLocationSimulated;

    // Status history
    private List<StatusHistoryItem> statusHistory;

    public EmergencyResponseDto() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReportedByName() { return reportedByName; }
    public void setReportedByName(String reportedByName) { this.reportedByName = reportedByName; }

    public Long getReportedByUserId() { return reportedByUserId; }
    public void setReportedByUserId(Long reportedByUserId) { this.reportedByUserId = reportedByUserId; }

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

    public String getAssignedHospitalName() { return assignedHospitalName; }
    public void setAssignedHospitalName(String assignedHospitalName) { this.assignedHospitalName = assignedHospitalName; }

    public Long getAssignedHospitalId() { return assignedHospitalId; }
    public void setAssignedHospitalId(Long assignedHospitalId) { this.assignedHospitalId = assignedHospitalId; }

    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    public Long getAssignedDoctorId() { return assignedDoctorId; }
    public void setAssignedDoctorId(Long assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

    public String getAssignedAmbulanceNumber() { return assignedAmbulanceNumber; }
    public void setAssignedAmbulanceNumber(String assignedAmbulanceNumber) { this.assignedAmbulanceNumber = assignedAmbulanceNumber; }

    public Long getAssignedAmbulanceId() { return assignedAmbulanceId; }
    public void setAssignedAmbulanceId(Long assignedAmbulanceId) { this.assignedAmbulanceId = assignedAmbulanceId; }

    public boolean isDoctorAcknowledged() { return doctorAcknowledged; }
    public void setDoctorAcknowledged(boolean doctorAcknowledged) { this.doctorAcknowledged = doctorAcknowledged; }

    public Integer getEstimatedArrivalMinutes() { return estimatedArrivalMinutes; }
    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) { this.estimatedArrivalMinutes = estimatedArrivalMinutes; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getDoctorAssignedAt() { return doctorAssignedAt; }
    public void setDoctorAssignedAt(LocalDateTime doctorAssignedAt) { this.doctorAssignedAt = doctorAssignedAt; }

    public LocalDateTime getAmbulanceDispatchedAt() { return ambulanceDispatchedAt; }
    public void setAmbulanceDispatchedAt(LocalDateTime ambulanceDispatchedAt) { this.ambulanceDispatchedAt = ambulanceDispatchedAt; }

    public Double getAmbulanceLatitude() { return ambulanceLatitude; }
    public void setAmbulanceLatitude(Double ambulanceLatitude) { this.ambulanceLatitude = ambulanceLatitude; }

    public Double getAmbulanceLongitude() { return ambulanceLongitude; }
    public void setAmbulanceLongitude(Double ambulanceLongitude) { this.ambulanceLongitude = ambulanceLongitude; }

    public LocalDateTime getAmbulanceLocationUpdatedAt() { return ambulanceLocationUpdatedAt; }
    public void setAmbulanceLocationUpdatedAt(LocalDateTime ambulanceLocationUpdatedAt) { this.ambulanceLocationUpdatedAt = ambulanceLocationUpdatedAt; }

    public boolean isAmbulanceLocationSimulated() { return ambulanceLocationSimulated; }
    public void setAmbulanceLocationSimulated(boolean ambulanceLocationSimulated) { this.ambulanceLocationSimulated = ambulanceLocationSimulated; }

    public List<StatusHistoryItem> getStatusHistory() { return statusHistory; }
    public void setStatusHistory(List<StatusHistoryItem> statusHistory) { this.statusHistory = statusHistory; }

    /**
     * Nested class for status history items in the response.
     */
    public static class StatusHistoryItem {
        private String fromStatus;
        private String toStatus;
        private String notes;
        private LocalDateTime changedAt;

        public StatusHistoryItem() {}

        public StatusHistoryItem(String fromStatus, String toStatus, String notes, LocalDateTime changedAt) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.notes = notes;
            this.changedAt = changedAt;
        }

        public String getFromStatus() { return fromStatus; }
        public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
        public String getToStatus() { return toStatus; }
        public void setToStatus(String toStatus) { this.toStatus = toStatus; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public LocalDateTime getChangedAt() { return changedAt; }
        public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
    }
}

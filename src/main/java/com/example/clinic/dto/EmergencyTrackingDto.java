package com.example.clinic.dto;

import com.example.clinic.entity.EmergencyStatus;
import java.time.LocalDateTime;

public class EmergencyTrackingDto {
    private Long emergencyId;
    private EmergencyStatus status;
    private String hospitalName;
    private String hospitalPhone;
    private String doctorName;
    private String ambulanceNumber;
    private Integer estimatedArrivalMinutes;
    private Double ambulanceLatitude;
    private Double ambulanceLongitude;
    private Double speedKmph;
    private boolean simulated;
    private LocalDateTime lastLocationUpdate;

    public EmergencyTrackingDto() {}

    public Long getEmergencyId() {
        return emergencyId;
    }

    public void setEmergencyId(Long emergencyId) {
        this.emergencyId = emergencyId;
    }

    public EmergencyStatus getStatus() {
        return status;
    }

    public void setStatus(EmergencyStatus status) {
        this.status = status;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalPhone() {
        return hospitalPhone;
    }

    public void setHospitalPhone(String hospitalPhone) {
        this.hospitalPhone = hospitalPhone;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getAmbulanceNumber() {
        return ambulanceNumber;
    }

    public void setAmbulanceNumber(String ambulanceNumber) {
        this.ambulanceNumber = ambulanceNumber;
    }

    public Integer getEstimatedArrivalMinutes() {
        return estimatedArrivalMinutes;
    }

    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) {
        this.estimatedArrivalMinutes = estimatedArrivalMinutes;
    }

    public Double getAmbulanceLatitude() {
        return ambulanceLatitude;
    }

    public void setAmbulanceLatitude(Double ambulanceLatitude) {
        this.ambulanceLatitude = ambulanceLatitude;
    }

    public Double getAmbulanceLongitude() {
        return ambulanceLongitude;
    }

    public void setAmbulanceLongitude(Double ambulanceLongitude) {
        this.ambulanceLongitude = ambulanceLongitude;
    }

    public Double getSpeedKmph() {
        return speedKmph;
    }

    public void setSpeedKmph(Double speedKmph) {
        this.speedKmph = speedKmph;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public LocalDateTime getLastLocationUpdate() {
        return lastLocationUpdate;
    }

    public void setLastLocationUpdate(LocalDateTime lastLocationUpdate) {
        this.lastLocationUpdate = lastLocationUpdate;
    }
}

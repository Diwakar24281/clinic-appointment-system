package com.example.clinic.dto;

/**
 * DTO for assigning a doctor and ambulance to an accepted emergency.
 */
public class EmergencyAssignDto {
    private Long hospitalId;
    private Long doctorId;
    private Long ambulanceId;
    private Long userId; // authenticated hospital user
    private Integer estimatedArrivalMinutes;

    public EmergencyAssignDto() {}

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public Long getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(Long ambulanceId) { this.ambulanceId = ambulanceId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getEstimatedArrivalMinutes() { return estimatedArrivalMinutes; }
    public void setEstimatedArrivalMinutes(Integer estimatedArrivalMinutes) { this.estimatedArrivalMinutes = estimatedArrivalMinutes; }
}

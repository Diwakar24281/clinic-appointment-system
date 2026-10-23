package com.example.clinic.dto;

/**
 * DTO for hospital acceptance of an emergency.
 */
public class EmergencyAcceptDto {
    private Long hospitalId;
    private Long userId;  // authenticated hospital user

    public EmergencyAcceptDto() {}

    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

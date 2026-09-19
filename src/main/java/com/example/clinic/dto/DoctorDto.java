package com.example.clinic.dto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorDto {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private Long specializationId;
    private String specializationName;
    private BigDecimal consultationFee;
    private boolean isAvailable;
    private List<DoctorAvailabilityDto> availabilities;

    public DoctorDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getSpecializationId() {
        return specializationId;
    }

    public void setSpecializationId(Long specializationId) {
        this.specializationId = specializationId;
    }

    public String getSpecializationName() {
        return specializationName;
    }

    public void setSpecializationName(String specializationName) {
        this.specializationName = specializationName;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public List<DoctorAvailabilityDto> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(List<DoctorAvailabilityDto> availabilities) {
        this.availabilities = availabilities;
    }
}

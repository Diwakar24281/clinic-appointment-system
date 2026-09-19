package com.example.clinic.dto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorCreateRequest {
    private String name;
    private String email;
    private String password;
    private Long specializationId;
    private BigDecimal consultationFee;
    private List<DoctorAvailabilityDto> availabilities;

    public DoctorCreateRequest() {
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getSpecializationId() {
        return specializationId;
    }

    public void setSpecializationId(Long specializationId) {
        this.specializationId = specializationId;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public List<DoctorAvailabilityDto> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(List<DoctorAvailabilityDto> availabilities) {
        this.availabilities = availabilities;
    }
}

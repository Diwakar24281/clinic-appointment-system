package com.example.clinic.dto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorUpdateRequest {
    private String name;
    private Long specializationId;
    private BigDecimal consultationFee;
    private Boolean isAvailable;
    private List<DoctorAvailabilityDto> availabilities;

    public DoctorUpdateRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public List<DoctorAvailabilityDto> getAvailabilities() {
        return availabilities;
    }

    public void setAvailabilities(List<DoctorAvailabilityDto> availabilities) {
        this.availabilities = availabilities;
    }
}

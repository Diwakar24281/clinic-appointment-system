package com.example.clinic.dto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorDto {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String approvalStatus;
    private Long specializationId;
    private String specializationName;
    private BigDecimal consultationFee;
    private boolean isAvailable;
    private List<DoctorAvailabilityDto> availabilities;
    private String todayAvailabilityStatus;
    private String todayWorkingHours;
    private String availableDaysSummary;
    private String availableDatesAndTimes;

    public DoctorDto() {
    }

    public String getTodayAvailabilityStatus() {
        return todayAvailabilityStatus;
    }

    public void setTodayAvailabilityStatus(String todayAvailabilityStatus) {
        this.todayAvailabilityStatus = todayAvailabilityStatus;
    }

    public String getTodayWorkingHours() {
        return todayWorkingHours;
    }

    public void setTodayWorkingHours(String todayWorkingHours) {
        this.todayWorkingHours = todayWorkingHours;
    }

    public String getAvailableDaysSummary() {
        return availableDaysSummary;
    }

    public void setAvailableDaysSummary(String availableDaysSummary) {
        this.availableDaysSummary = availableDaysSummary;
    }

    public String getAvailableDatesAndTimes() {
        return availableDatesAndTimes;
    }

    public void setAvailableDatesAndTimes(String availableDatesAndTimes) {
        this.availableDatesAndTimes = availableDatesAndTimes;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
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

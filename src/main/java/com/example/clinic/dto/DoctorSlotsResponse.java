package com.example.clinic.dto;

import java.time.LocalDate;
import java.util.List;

public class DoctorSlotsResponse {
    private Long doctorId;
    private String doctorName;
    private LocalDate date;
    private String dayOfWeek;
    private String workingHours;
    private List<SlotDto> slots;

    public DoctorSlotsResponse() {
    }

    public DoctorSlotsResponse(Long doctorId, String doctorName, LocalDate date, String dayOfWeek, String workingHours, List<SlotDto> slots) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.workingHours = workingHours;
        this.slots = slots;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(String workingHours) {
        this.workingHours = workingHours;
    }

    public List<SlotDto> getSlots() {
        return slots;
    }

    public void setSlots(List<SlotDto> slots) {
        this.slots = slots;
    }
}

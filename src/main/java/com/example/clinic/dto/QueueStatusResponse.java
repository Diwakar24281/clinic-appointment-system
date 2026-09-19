package com.example.clinic.dto;

import java.time.LocalDate;
import java.util.List;

public class QueueStatusResponse {
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private LocalDate date;
    private Integer currentToken;
    private Integer nextToken;
    private int waitingCount;
    private List<AppointmentResponse> queue;

    public QueueStatusResponse() {
    }

    public QueueStatusResponse(Long doctorId, String doctorName, String specialization, LocalDate date, Integer currentToken, Integer nextToken, int waitingCount, List<AppointmentResponse> queue) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.date = date;
        this.currentToken = currentToken;
        this.nextToken = nextToken;
        this.waitingCount = waitingCount;
        this.queue = queue;
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

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(Integer currentToken) {
        this.currentToken = currentToken;
    }

    public Integer getNextToken() {
        return nextToken;
    }

    public void setNextToken(Integer nextToken) {
        this.nextToken = nextToken;
    }

    public int getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(int waitingCount) {
        this.waitingCount = waitingCount;
    }

    public List<AppointmentResponse> getQueue() {
        return queue;
    }

    public void setQueue(List<AppointmentResponse> queue) {
        this.queue = queue;
    }
}

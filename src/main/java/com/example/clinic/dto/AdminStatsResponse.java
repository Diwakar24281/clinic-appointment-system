package com.example.clinic.dto;

public class AdminStatsResponse {
    private long totalDoctors;
    private long totalPatients;
    private long todayAppointments;
    private long totalCompletedAppointments;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(long totalDoctors, long totalPatients, long todayAppointments, long totalCompletedAppointments) {
        this.totalDoctors = totalDoctors;
        this.totalPatients = totalPatients;
        this.todayAppointments = todayAppointments;
        this.totalCompletedAppointments = totalCompletedAppointments;
    }

    public long getTotalDoctors() {
        return totalDoctors;
    }

    public void setTotalDoctors(long totalDoctors) {
        this.totalDoctors = totalDoctors;
    }

    public long getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(long totalPatients) {
        this.totalPatients = totalPatients;
    }

    public long getTodayAppointments() {
        return todayAppointments;
    }

    public void setTodayAppointments(long todayAppointments) {
        this.todayAppointments = todayAppointments;
    }

    public long getTotalCompletedAppointments() {
        return totalCompletedAppointments;
    }

    public void setTotalCompletedAppointments(long totalCompletedAppointments) {
        this.totalCompletedAppointments = totalCompletedAppointments;
    }
}

package com.example.clinic.dto;

import java.util.List;

public class PrescriptionRequest {
    private Long appointmentId;
    private String diagnosis;
    private String notes;
    private List<PrescriptionMedicineDto> medicines;

    public PrescriptionRequest() {
    }

    public PrescriptionRequest(Long appointmentId, String diagnosis, String notes, List<PrescriptionMedicineDto> medicines) {
        this.appointmentId = appointmentId;
        this.diagnosis = diagnosis;
        this.notes = notes;
        this.medicines = medicines;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PrescriptionMedicineDto> getMedicines() {
        return medicines;
    }

    public void setMedicines(List<PrescriptionMedicineDto> medicines) {
        this.medicines = medicines;
    }
}

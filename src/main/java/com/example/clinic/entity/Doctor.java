package com.example.clinic.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @ManyToOne(optional = true)
    @JoinColumn(name = "specialization_id", referencedColumnName = "id", nullable = true)
    private Specialization specialization;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true;

    @Column(name = "approval_status", length = 30, nullable = false)
    private String approvalStatus = "APPROVED"; // "APPROVED", "PENDING_APPROVAL", "INACTIVE"

    @Column(name = "phone", length = 20)
    private String phone;

    public Doctor() {
    }

    public Doctor(User user, Specialization specialization, BigDecimal consultationFee, boolean isAvailable) {
        this.user = user;
        this.specialization = specialization;
        this.consultationFee = consultationFee;
        this.isAvailable = isAvailable;
        this.approvalStatus = "APPROVED";
    }

    public Doctor(User user, Specialization specialization, BigDecimal consultationFee, boolean isAvailable, String approvalStatus) {
        this.user = user;
        this.specialization = specialization;
        this.consultationFee = consultationFee;
        this.isAvailable = isAvailable;
        this.approvalStatus = approvalStatus;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
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

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

package com.example.clinic.dto;

public class SendOtpRequest {
    private String phone;
    private String email;
    private String name;
    private String role; // "PATIENT" or "DOCTOR"
    private String purpose; // "REGISTRATION" or "PASSWORD_RESET"

    public SendOtpRequest() {
    }

    public SendOtpRequest(String phone, String email, String name, String role, String purpose) {
        this.phone = phone;
        this.email = email;
        this.name = name;
        this.role = role;
        this.purpose = purpose;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}

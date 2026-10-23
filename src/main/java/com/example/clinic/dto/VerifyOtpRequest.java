package com.example.clinic.dto;

public class VerifyOtpRequest {
    private String phone;
    private String email;
    private String otp;
    private String purpose; // "REGISTRATION" or "PASSWORD_RESET"

    public VerifyOtpRequest() {
    }

    public VerifyOtpRequest(String phone, String email, String otp, String purpose) {
        this.phone = phone;
        this.email = email;
        this.otp = otp;
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

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}

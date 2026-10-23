package com.example.clinic.dto;

public class SmsDiagnosticResult {
    private String provider;
    private boolean configured;
    private String status; // "READY", "MISSING_CREDENTIALS", "AUTHENTICATION_FAILED", "INSUFFICIENT_BALANCE", "TRIAL_RESTRICTION", "NETWORK_ERROR"
    private String message;
    private String maskedSender;
    private String maskedKeyOrSid;
    private String details;
    private String instructions;
    private String providerMessageId;

    public SmsDiagnosticResult() {
    }

    public SmsDiagnosticResult(String provider, boolean configured, String status, String message) {
        this.provider = provider;
        this.configured = configured;
        this.status = status;
        this.message = message;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMaskedSender() {
        return maskedSender;
    }

    public void setMaskedSender(String maskedSender) {
        this.maskedSender = maskedSender;
    }

    public String getMaskedKeyOrSid() {
        return maskedKeyOrSid;
    }

    public void setMaskedKeyOrSid(String maskedKeyOrSid) {
        this.maskedKeyOrSid = maskedKeyOrSid;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public void setProviderMessageId(String providerMessageId) {
        this.providerMessageId = providerMessageId;
    }
}

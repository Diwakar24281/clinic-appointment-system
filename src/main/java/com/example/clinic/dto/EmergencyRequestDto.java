package com.example.clinic.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for creating a new emergency request from the frontend.
 */
public class EmergencyRequestDto {

    @NotBlank(message = "Emergency description is required")
    @Size(max = 2000, message = "Description must be under 2000 characters")
    private String description;

    @Size(max = 100)
    private String category;

    @NotBlank(message = "Callback phone number is required")
    @Size(max = 20)
    private String callbackPhone;

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    private Double locationAccuracy;

    @Size(max = 500)
    private String manualAddress;

    @Size(max = 200)
    private String landmark;

    @Size(max = 200)
    private String area;

    // The userId of the authenticated reporter
    private Long userId;

    public EmergencyRequestDto() {}

    // Getters and Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCallbackPhone() { return callbackPhone; }
    public void setCallbackPhone(String callbackPhone) { this.callbackPhone = callbackPhone; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getLocationAccuracy() { return locationAccuracy; }
    public void setLocationAccuracy(Double locationAccuracy) { this.locationAccuracy = locationAccuracy; }

    public String getManualAddress() { return manualAddress; }
    public void setManualAddress(String manualAddress) { this.manualAddress = manualAddress; }

    public String getLandmark() { return landmark; }
    public void setLandmark(String landmark) { this.landmark = landmark; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}

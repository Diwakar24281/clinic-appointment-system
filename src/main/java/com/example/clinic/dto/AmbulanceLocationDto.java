package com.example.clinic.dto;

import jakarta.validation.constraints.*;

/**
 * DTO for submitting ambulance GPS location updates.
 */
public class AmbulanceLocationDto {

    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    @NotNull
    private Double latitude;

    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    @NotNull
    private Double longitude;

    private Double speedKmph;
    private boolean simulated = false;

    public AmbulanceLocationDto() {}

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getSpeedKmph() { return speedKmph; }
    public void setSpeedKmph(Double speedKmph) { this.speedKmph = speedKmph; }

    public boolean isSimulated() { return simulated; }
    public void setSimulated(boolean simulated) { this.simulated = simulated; }
}

package com.example.clinic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Participating hospital that can receive and respond to emergency alerts.
 * Each hospital has a user account (Role.HOSPITAL) for dashboard login.
 */
@Entity
@Table(name = "hospitals")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    @Column(nullable = false, length = 200)
    @NotBlank
    private String name;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String area;

    @Column(length = 20)
    private String phone;

    // Hospital GPS coordinates for proximity matching
    @Column(precision = 9)
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;

    @Column(precision = 9)
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    // Service radius in kilometers
    @Column(name = "service_radius_km")
    @Min(1)
    private Double serviceRadiusKm = 15.0;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "has_emergency_services", nullable = false)
    private boolean hasEmergencyServices = true;

    public Hospital() {}

    public Hospital(User user, String name, String address, String area, String phone,
                    Double latitude, Double longitude, Double serviceRadiusKm) {
        this.user = user;
        this.name = name;
        this.address = address;
        this.area = area;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.serviceRadiusKm = serviceRadiusKm;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getServiceRadiusKm() { return serviceRadiusKm; }
    public void setServiceRadiusKm(Double serviceRadiusKm) { this.serviceRadiusKm = serviceRadiusKm; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isHasEmergencyServices() { return hasEmergencyServices; }
    public void setHasEmergencyServices(boolean hasEmergencyServices) { this.hasEmergencyServices = hasEmergencyServices; }
}

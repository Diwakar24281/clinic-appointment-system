package com.example.clinic.repository;

import com.example.clinic.entity.AmbulanceLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AmbulanceLocationRepository extends JpaRepository<AmbulanceLocation, Long> {
    Optional<AmbulanceLocation> findTopByEmergencyRequestIdOrderByRecordedAtDesc(Long emergencyId);
}

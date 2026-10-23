package com.example.clinic.repository;

import com.example.clinic.entity.HospitalResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalResponseRepository extends JpaRepository<HospitalResponse, Long> {
    List<HospitalResponse> findByEmergencyRequestId(Long emergencyId);
    Optional<HospitalResponse> findByEmergencyRequestIdAndHospitalId(Long emergencyId, Long hospitalId);
    boolean existsByEmergencyRequestIdAndHospitalIdAndResponseType(Long emergencyId, Long hospitalId, String responseType);
}

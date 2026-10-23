package com.example.clinic.repository;

import com.example.clinic.entity.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {
    List<Ambulance> findByHospitalIdAndIsAvailableTrue(Long hospitalId);
    List<Ambulance> findByHospitalId(Long hospitalId);
}

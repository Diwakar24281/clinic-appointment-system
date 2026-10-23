package com.example.clinic.repository;

import com.example.clinic.entity.EmergencyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyAssignmentRepository extends JpaRepository<EmergencyAssignment, Long> {
    Optional<EmergencyAssignment> findByEmergencyRequestId(Long emergencyId);
    List<EmergencyAssignment> findByDoctorIdAndDoctorAcknowledgedFalse(Long doctorId);
    List<EmergencyAssignment> findByHospitalId(Long hospitalId);
    List<EmergencyAssignment> findByDoctorId(Long doctorId);
}

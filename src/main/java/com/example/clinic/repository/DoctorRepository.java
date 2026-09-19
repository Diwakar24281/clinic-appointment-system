package com.example.clinic.repository;

import com.example.clinic.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUserId(Long userId);
    List<Doctor> findBySpecializationId(Long specializationId);
    List<Doctor> findByIsAvailableTrue();
    List<Doctor> findBySpecializationIdAndIsAvailableTrue(Long specializationId);
}

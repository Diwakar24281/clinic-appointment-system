package com.example.clinic.repository;

import com.example.clinic.entity.EmergencyStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmergencyStatusHistoryRepository extends JpaRepository<EmergencyStatusHistory, Long> {
    List<EmergencyStatusHistory> findByEmergencyRequestIdOrderByChangedAtAsc(Long emergencyId);
}

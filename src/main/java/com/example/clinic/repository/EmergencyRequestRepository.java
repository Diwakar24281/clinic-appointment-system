package com.example.clinic.repository;

import com.example.clinic.entity.EmergencyRequest;
import com.example.clinic.entity.EmergencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {

    List<EmergencyRequest> findByReportedByIdOrderByCreatedAtDesc(Long userId);

    List<EmergencyRequest> findByStatusInOrderByCreatedAtDesc(List<EmergencyStatus> statuses);

    /**
     * Atomic status update: only succeeds if current status matches expected status.
     * This is the core concurrency control for first-acceptance-wins.
     * Returns the number of rows updated (0 = already changed by another hospital).
     */
    @Modifying
    @Query("UPDATE EmergencyRequest e SET e.status = :newStatus, e.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE e.id = :id AND e.status = :expectedStatus")
    int updateStatusAtomically(@Param("id") Long id,
                               @Param("expectedStatus") EmergencyStatus expectedStatus,
                               @Param("newStatus") EmergencyStatus newStatus);

    List<EmergencyRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Optional<EmergencyRequest> findTopByReportedByIdAndStatusNotInOrderByCreatedAtDesc(
            Long userId, List<EmergencyStatus> excludedStatuses);
}

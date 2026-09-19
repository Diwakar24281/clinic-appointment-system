package com.example.clinic.repository;

import com.example.clinic.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
    Optional<Specialization> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}

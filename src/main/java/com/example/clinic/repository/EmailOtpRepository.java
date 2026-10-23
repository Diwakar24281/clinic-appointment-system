package com.example.clinic.repository;

import com.example.clinic.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, Long> {

    Optional<EmailOtp> findTopByEmailAndPurposeOrderByIdDesc(String email, String purpose);

    Optional<EmailOtp> findByVerificationToken(String verificationToken);

    List<EmailOtp> findByEmailAndPurposeAndVerifiedFalse(String email, String purpose);
}

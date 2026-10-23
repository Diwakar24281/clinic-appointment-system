package com.example.clinic.repository;

import com.example.clinic.entity.MobileOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MobileOtpRepository extends JpaRepository<MobileOtp, Long> {

    Optional<MobileOtp> findTopByMobileNumberAndPurposeOrderByIdDesc(String mobileNumber, String purpose);

    Optional<MobileOtp> findTopByEmailAndPurposeOrderByIdDesc(String email, String purpose);

    Optional<MobileOtp> findByVerificationToken(String verificationToken);

    List<MobileOtp> findByMobileNumberAndPurposeAndVerifiedFalse(String mobileNumber, String purpose);

    List<MobileOtp> findByEmailAndPurposeAndVerifiedFalse(String email, String purpose);
}

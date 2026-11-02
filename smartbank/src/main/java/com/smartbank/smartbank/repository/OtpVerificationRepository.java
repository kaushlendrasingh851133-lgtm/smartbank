package com.smartbank.smartbank.repository;

import com.smartbank.smartbank.entity.OtpPurpose;
import com.smartbank.smartbank.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OtpVerificationRepository
        extends JpaRepository<OtpVerification, Long> {

    List<OtpVerification> findByUser_IdAndPurposeOrderByCreatedAtDesc(
            Long userId,
            OtpPurpose purpose
    );

    Optional<OtpVerification>
    findFirstByUser_IdAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
            Long userId,
            OtpPurpose purpose
    );
}
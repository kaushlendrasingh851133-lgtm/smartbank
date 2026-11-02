package com.smartbank.smartbank.service;

import com.smartbank.smartbank.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpAttemptService {

    private final OtpVerificationRepository otpRepository;

    public OtpAttemptService(
            OtpVerificationRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int incrementAttempts(Long otpId) {

        var verification = otpRepository.findById(otpId)
                .orElseThrow(() ->
                        new IllegalArgumentException("OTP record not found."));

        verification.setAttempts(verification.getAttempts() + 1);

        otpRepository.saveAndFlush(verification);

        return verification.getAttempts();
    }
}
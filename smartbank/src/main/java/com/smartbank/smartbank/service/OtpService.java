package com.smartbank.smartbank.service;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.AccountStatus;
import com.smartbank.smartbank.entity.OtpPurpose;
import com.smartbank.smartbank.entity.OtpVerification;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.entity.VerificationStatus;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.OtpVerificationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpVerificationRepository otpRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpAttemptService otpAttemptService;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpVerificationRepository otpRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            OtpAttemptService otpAttemptService) {

        this.otpRepository = otpRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpAttemptService = otpAttemptService;
    }

    @Transactional
    public String generateOtp(User user, OtpPurpose purpose) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Valid user is required.");
        }

        if (purpose == null) {
            throw new IllegalArgumentException("OTP purpose is required.");
        }

        Account account = accountRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found."));

        if (purpose == OtpPurpose.ACCOUNT_VERIFICATION
                && account.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException("Your account is already verified.");
        }

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Your account is not active.");
        }

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        OtpVerification verification = new OtpVerification();
        verification.setUser(user);
        verification.setOtpHash(passwordEncoder.encode(otp));
        verification.setPurpose(purpose);
        verification.setExpiresAt(
                LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES)
        );
        verification.setVerified(false);
        verification.setAttempts(0);

        otpRepository.saveAndFlush(verification);

        return otp;
    }

    @Transactional
    public void verifyAccountOtp(Long userId, String submittedOtp) {

        if (userId == null) {
            throw new IllegalArgumentException("User identification is required.");
        }

        if (submittedOtp == null || !submittedOtp.matches("\\d{6}")) {
            throw new IllegalArgumentException("Enter a valid 6-digit OTP.");
        }

        OtpVerification verification = otpRepository
                .findFirstByUser_IdAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                        userId,
                        OtpPurpose.ACCOUNT_VERIFICATION
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No active OTP found. Request a new OTP."
                        ));

        if (verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "Maximum OTP attempts reached. Request a new OTP."
            );
        }

        if (verification.getExpiresAt() == null
                || !LocalDateTime.now().isBefore(verification.getExpiresAt())) {
            throw new IllegalArgumentException("OTP expired. Request a new OTP.");
        }

        Account account = accountRepository.findByUser_Id(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found."));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Your account is not active.");
        }

        int attempts = otpAttemptService.incrementAttempts(verification.getId());

        if (!passwordEncoder.matches(
                submittedOtp,
                verification.getOtpHash())) {

            throw new IllegalArgumentException(
                    attempts >= MAX_ATTEMPTS
                            ? "Maximum OTP attempts reached. Request a new OTP."
                            : "Incorrect OTP. Please try again."
            );
        }

        verification.setVerified(true);
        otpRepository.saveAndFlush(verification);

        account.setVerificationStatus(VerificationStatus.VERIFIED);
        accountRepository.save(account);
    }
}
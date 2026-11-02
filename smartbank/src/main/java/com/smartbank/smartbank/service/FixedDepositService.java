
package com.smartbank.smartbank.service;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.FixedDeposit;
import com.smartbank.smartbank.entity.FixedDepositStatus;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.FixedDepositRepository;
import com.smartbank.smartbank.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FixedDepositService {

    private static final BigDecimal ANNUAL_RATE =
            new BigDecimal("6.00");

    private final FixedDepositRepository fixedDepositRepository;
    private final UserRepository userRepository;
    private final BankingService bankingService;

    public FixedDepositService(
            FixedDepositRepository fixedDepositRepository,
            UserRepository userRepository,
            BankingService bankingService) {

        this.fixedDepositRepository = fixedDepositRepository;
        this.userRepository = userRepository;
        this.bankingService = bankingService;
    }

    // Calculate simple interest at 6% per annum
    public BigDecimal calculateInterest(
            BigDecimal principal, int months) {

        if (principal == null
                || principal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "FD amount must be greater than zero.");
        }

        if (months != 6 && months != 12
                && months != 24 && months != 36) {
            throw new IllegalArgumentException(
                    "Select a valid FD duration.");
        }

        return principal
                .multiply(ANNUAL_RATE)
                .multiply(BigDecimal.valueOf(months))
                .divide(new BigDecimal("1200"), 2,
                        RoundingMode.HALF_UP);
    }

    // Create a fixed deposit
    @Transactional
    public FixedDeposit createFD(
            Long userId, BigDecimal amount, int months) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.compareTo(
                new BigDecimal("1000000.00")) > 0
                || amount.scale() > 2) {

            throw new IllegalArgumentException(
                    "Enter a valid FD amount (maximum ₹10,00,000).");
        }

        if (months != 6 && months != 12
                && months != 24 && months != 36) {
            throw new IllegalArgumentException(
                    "Invalid FD duration.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."));

        Account account =
                bankingService.getVerifiedAccount(userId);

        BigDecimal principal =
                amount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal interest =
                calculateInterest(principal, months);

        BigDecimal maturity = principal.add(interest)
                .setScale(2, RoundingMode.HALF_UP);

        LocalDate start = LocalDate.now();

        FixedDeposit fd = new FixedDeposit();
        fd.setUser(user);
        fd.setAccount(account);
        fd.setPrincipalAmount(principal);
        fd.setInterestRate(ANNUAL_RATE);
        fd.setDurationMonths(months);
        fd.setInterestAmount(interest);
        fd.setMaturityAmount(maturity);
        fd.setStartDate(start);
        fd.setMaturityDate(start.plusMonths(months));
        fd.setStatus(FixedDepositStatus.ACTIVE);
        fd.setCreatedAt(LocalDateTime.now());

        // Deduct the principal from the account.
        bankingService.withdraw(userId, principal);

        return fixedDepositRepository.saveAndFlush(fd);
    }

    // Get all FDs belonging to a user
    @Transactional(readOnly = true)
    public List<FixedDeposit> getUserFDs(Long userId) {
        return fixedDepositRepository
                .findByUser_IdOrderByCreatedAtDesc(userId);
    }

    // Withdraw an FD only after its maturity date.
    @Transactional
    public void withdrawMaturedFD(Long userId, Long fdId) {

        FixedDeposit fd = fixedDepositRepository
                .findById(fdId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Fixed Deposit not found."));

        // Ensure the FD belongs to the logged-in user.
        if (!fd.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "You are not allowed to withdraw this FD.");
        }

        // Prevent early or duplicate withdrawals.
        if (fd.getStatus() != FixedDepositStatus.ACTIVE) {
            throw new IllegalStateException(
                    "This FD has already been withdrawn or processed.");
        }

        if (LocalDate.now().isBefore(fd.getMaturityDate())) {
            throw new IllegalStateException(
                    "FD can only be withdrawn on or after "
                            + fd.getMaturityDate() + ".");
        }

        // Credit principal plus the pre-calculated 6% simple interest.
        bankingService.deposit(
                userId, fd.getMaturityAmount());

        fd.setStatus(FixedDepositStatus.MATURED);
        fd.setMaturedAt(LocalDateTime.now());

        fixedDepositRepository.saveAndFlush(fd);
    }

    // Process all active FDs that have reached maturity.
    @Transactional
    public void processMaturedFDs() {

        List<FixedDeposit> maturedFDs =
                fixedDepositRepository.findMaturedForUpdate(
                        FixedDepositStatus.ACTIVE,
                        LocalDate.now());

        for (FixedDeposit fd : maturedFDs) {

            bankingService.deposit(
                    fd.getUser().getId(),
                    fd.getMaturityAmount());

            fd.setStatus(FixedDepositStatus.MATURED);
            fd.setMaturedAt(LocalDateTime.now());

            fixedDepositRepository.save(fd);
        }

        fixedDepositRepository.flush();
    }
}

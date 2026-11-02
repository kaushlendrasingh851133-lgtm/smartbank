
package com.smartbank.smartbank.service;

import com.smartbank.smartbank.entity.Loan;
import com.smartbank.smartbank.entity.LoanRepayment;
import com.smartbank.smartbank.entity.LoanStatus;
import com.smartbank.smartbank.entity.RepaymentStatus;

import com.smartbank.smartbank.repository.LoanRepaymentRepository;
import com.smartbank.smartbank.repository.LoanRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanRepaymentService {

    private static final BigDecimal ANNUAL_RATE =
            new BigDecimal("0.10");

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final BankingService bankingService;

    public LoanRepaymentService(
            LoanRepository loanRepository,
            LoanRepaymentRepository repaymentRepository,
            BankingService bankingService) {

        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.bankingService = bankingService;
    }

    public BigDecimal calculateMonthlyEmi(
            BigDecimal principal, int months) {

        if (principal == null
                || principal.compareTo(BigDecimal.ZERO) <= 0
                || months <= 0) {
            throw new IllegalArgumentException(
                    "Loan amount and duration must be positive.");
        }

        BigDecimal monthlyRate = ANNUAL_RATE.divide(
                BigDecimal.valueOf(12),
                12,
                RoundingMode.HALF_UP);

        double rate = monthlyRate.doubleValue();
        double amount = principal.doubleValue();

        double factor = Math.pow(1 + rate, months);

        double emi = amount * rate * factor / (factor - 1);

        return BigDecimal.valueOf(emi)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public void generateSchedule(Long loanId) {

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Loan not found."));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved loans can have a repayment schedule.");
        }

        if (repaymentRepository
                .existsByLoanAndInstallmentNumber(loan, 1)) {
            return;
        }

        int months = loan.getDurationMonths();
        BigDecimal principal = loan.getLoanAmount();

        BigDecimal emi = calculateMonthlyEmi(principal, months);

        BigDecimal monthlyRate = ANNUAL_RATE.divide(
                BigDecimal.valueOf(12),
                12,
                RoundingMode.HALF_UP);

        BigDecimal remaining = principal;
        LocalDate firstDueDate = LocalDate.now().plusMonths(1);

        for (int i = 1; i <= months; i++) {

            BigDecimal interest = remaining.multiply(monthlyRate)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal principalPart = emi.subtract(interest)
                    .setScale(2, RoundingMode.HALF_UP);

            if (i == months
                    || principalPart.compareTo(remaining) > 0) {
                principalPart = remaining;
            }

            BigDecimal installmentAmount = principalPart
                    .add(interest)
                    .setScale(2, RoundingMode.HALF_UP);

            LoanRepayment repayment = new LoanRepayment();

            repayment.setLoan(loan);
            repayment.setInstallmentNumber(i);
            repayment.setDueDate(firstDueDate.plusMonths(i - 1));
            repayment.setEmiAmount(installmentAmount);
            repayment.setPaidAmount(BigDecimal.ZERO);
            repayment.setPaymentStatus(RepaymentStatus.PENDING);

            repaymentRepository.save(repayment);

            remaining = remaining.subtract(principalPart)
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    // Pay one EMI securely in a single database transaction
    @Transactional
    public void payInstallment(Long userId, Long repaymentId) {

        LoanRepayment repayment = repaymentRepository
                .findByIdAndLoan_User_IdForUpdate(
                        repaymentId, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Repayment record not found."));

        if (repayment.getLoan().getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException(
                    "Only approved loans can be repaid.");
        }

        if (repayment.getPaymentStatus() == RepaymentStatus.PAID) {
            throw new IllegalStateException(
                    "This EMI has already been paid.");
        }

        // Deduct balance and create transaction history entry
        // Any failure rolls back the complete payment operation.
        bankingService.payLoanRepayment(
                userId, repayment.getEmiAmount());

        repayment.setPaidAmount(repayment.getEmiAmount());
        repayment.setPaymentStatus(RepaymentStatus.PAID);
        repayment.setPaidAt(LocalDateTime.now());

        repaymentRepository.saveAndFlush(repayment);
    }

    @Transactional
    public void updateOverdueRepayments() {

        LocalDate today = LocalDate.now();

        List<LoanRepayment> repayments = repaymentRepository.findAll();

        for (LoanRepayment repayment : repayments) {

            if (repayment.getPaymentStatus() == RepaymentStatus.PENDING
                    && repayment.getDueDate().isBefore(today)) {

                repayment.setPaymentStatus(RepaymentStatus.OVERDUE);
                repaymentRepository.save(repayment);
            }
        }
    }
}

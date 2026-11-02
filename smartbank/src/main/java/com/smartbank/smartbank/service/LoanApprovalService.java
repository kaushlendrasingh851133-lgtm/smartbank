package com.smartbank.smartbank.service;

import com.smartbank.smartbank.entity.Loan;
import com.smartbank.smartbank.entity.LoanStatus;
import com.smartbank.smartbank.repository.LoanRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoanApprovalService {

    private final LoanRepository loanRepository;
    private final BankingService bankingService;
    private final LoanRepaymentService loanRepaymentService;

    public LoanApprovalService(
            LoanRepository loanRepository,
            BankingService bankingService,
            LoanRepaymentService loanRepaymentService) {
        this.loanRepository = loanRepository;
        this.bankingService = bankingService;
        this.loanRepaymentService = loanRepaymentService;
    }

    @Transactional
    public void approveLoan(Long loanId, String adminRemark) {

        Loan loan = loanRepository.findByIdWithUserForUpdate(loanId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Loan application not found."));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This loan has already been reviewed.");
        }

        if (loan.getDisbursedAt() != null) {
            throw new IllegalArgumentException(
                    "This loan has already been disbursed. Contact support.");
        }

        // Credit account and save LOAN_DISBURSEMENT transaction.
        bankingService.disburseLoan(
                loan.getUser().getId(),
                loan.getLoanAmount(),
                loan.getId()
        );

        loan.setStatus(LoanStatus.APPROVED);
        loan.setAdminRemark(adminRemark);
        loan.setReviewedAt(LocalDateTime.now());
        loan.setDisbursedAt(LocalDateTime.now());

        loanRepository.saveAndFlush(loan);

        // Generate EMI schedule in the same transaction.
        loanRepaymentService.generateSchedule(loan.getId());
    }

    @Transactional
    public Loan rejectLoan(Long loanId, String adminRemark) {

        Loan loan = loanRepository.findByIdWithUserForUpdate(loanId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Loan application not found."));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This loan has already been reviewed.");
        }

        loan.setStatus(LoanStatus.REJECTED);
        loan.setAdminRemark(adminRemark);
        loan.setReviewedAt(LocalDateTime.now());

        return loanRepository.save(loan);
    }
}

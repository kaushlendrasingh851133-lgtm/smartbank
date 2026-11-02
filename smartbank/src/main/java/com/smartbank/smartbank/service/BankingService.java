package com.smartbank.smartbank.service;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.AccountStatus;
import com.smartbank.smartbank.entity.VerificationStatus;
import com.smartbank.smartbank.entity.Transaction;
import com.smartbank.smartbank.entity.Loan;

import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.TransactionRepository;
import com.smartbank.smartbank.repository.LoanRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BankingService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;

    public BankingService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LoanRepository loanRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRepository = loanRepository;
    }

    // Validation for regular banking transactions: maximum ₹10 lakh
    private void validateAmount(BigDecimal amount) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero.");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Maximum two decimal places are allowed.");
        }

        if (amount.compareTo(new BigDecimal("1000000.00")) > 0) {
            throw new IllegalArgumentException(
                    "Transaction limit exceeded.");
        }

        if (amount.precision() > 15) {
            throw new IllegalArgumentException(
                    "Amount is too large.");
        }
    }

    // Separate validation for loans: maximum ₹15 lakh
    private void validateLoanAmount(BigDecimal amount) {

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Loan amount must be greater than zero.");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Loan amount can have maximum two decimal places.");
        }

        if (amount.compareTo(new BigDecimal("1500000.00")) > 0) {
            throw new IllegalArgumentException(
                    "Maximum loan amount is ₹15,00,000.");
        }

        if (amount.precision() > 15) {
            throw new IllegalArgumentException(
                    "Loan amount is too large.");
        }
    }

    public Account getVerifiedAccount(Long userId) {

        Account existingAccount = accountRepository
                .findByUser_Id(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bank account not found."));

        Account account = accountRepository
                .findByIdForUpdate(existingAccount.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bank account not found."));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Your account is not active.");
        }

        if (account.getVerificationStatus()
                != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException(
                    "Account verification is required before transactions.");
        }

        return account;
    }

    // Deposit money
    @Transactional
    public void deposit(Long userId, BigDecimal amount) {

        validateAmount(amount);

        Account account = getVerifiedAccount(userId);
        BigDecimal newBalance = account.getBalance().add(amount);

        if (newBalance.precision() > 15) {
            throw new IllegalArgumentException(
                    "Balance limit exceeded.");
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        transaction.setTransactionType("DEPOSIT");
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Deposit");
        transaction.setReceiverAccount(account);

        transactionRepository.save(transaction);
    }

    // Withdraw money
    @Transactional
    public void withdraw(Long userId, BigDecimal amount) {

        validateAmount(amount);

        Account account = getVerifiedAccount(userId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance.");
        }

        BigDecimal newBalance = account.getBalance().subtract(amount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        transaction.setTransactionType("WITHDRAW");
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Withdrawal");
        transaction.setSenderAccount(account);

        transactionRepository.save(transaction);
    }

    // Credit an approved loan exactly once
    @Transactional
    public void disburseLoan(
            Long userId,
            BigDecimal loanAmount,
            Long loanId) {

        validateLoanAmount(loanAmount);

        if (loanId == null) {
            throw new IllegalArgumentException(
                    "Loan ID is required.");
        }

        // Database-backed duplicate check
        if (transactionRepository.existsByLoan_Id(loanId)) {
            throw new IllegalArgumentException(
                    "A disbursement transaction already exists for this loan.");
        }

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Loan application not found."));

        // Verify the loan belongs to the supplied customer
        if (loan.getUser() == null
                || !loan.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException(
                    "This loan does not belong to the customer.");
        }

        // Verify the requested amount matches the saved loan
        if (loan.getLoanAmount() == null
                || loan.getLoanAmount().compareTo(loanAmount) != 0) {
            throw new IllegalArgumentException(
                    "Loan amount does not match the application.");
        }

        Account account = getVerifiedAccount(userId);

        BigDecimal newBalance = account.getBalance().add(loanAmount);

        if (newBalance.precision() > 15) {
            throw new IllegalArgumentException(
                    "Balance limit exceeded.");
        }

        // Update account balance
        account.setBalance(newBalance);
        accountRepository.save(account);

        // Save transaction linked to the specific loan
        Transaction transaction = new Transaction();
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        transaction.setTransactionType("LOAN_DISBURSEMENT");
        transaction.setAmount(loanAmount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription(
                "Loan disbursement for Loan ID: " + loanId);
        transaction.setReceiverAccount(account);
        transaction.setLoan(loan);

        transactionRepository.saveAndFlush(transaction);
    }

    // Deduct monthly loan EMI
    @Transactional
    public void payLoanRepayment(
            Long userId,
            BigDecimal emiAmount) {

        validateAmount(emiAmount);

        Account account = getVerifiedAccount(userId);

        if (account.getBalance().compareTo(emiAmount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance to pay the loan EMI.");
        }

        BigDecimal newBalance = account.getBalance().subtract(emiAmount);
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setReferenceNumber(UUID.randomUUID().toString());
        transaction.setTransactionType("LOAN_REPAYMENT");
        transaction.setAmount(emiAmount);
        transaction.setStatus("SUCCESS");
        transaction.setDescription("Monthly loan EMI repayment");
        transaction.setSenderAccount(account);

        transactionRepository.save(transaction);
    }
}

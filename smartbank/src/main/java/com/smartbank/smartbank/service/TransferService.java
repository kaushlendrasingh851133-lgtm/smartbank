package com.smartbank.smartbank.service;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.AccountStatus;
import com.smartbank.smartbank.entity.Transaction;
import com.smartbank.smartbank.entity.VerificationStatus;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final SecureRandom random = new SecureRandom();

    public TransferService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public String transfer(
            Long senderUserId,
            String receiverAccountNumber,
            BigDecimal amount) {

        // Validate amount
        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero."
            );
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Maximum two decimal places are allowed."
            );
        }

        if (amount.compareTo(new BigDecimal("1000000.00")) > 0) {
            throw new IllegalArgumentException(
                    "Transfer limit exceeded."
            );
        }

        if (amount.precision() > 15) {
            throw new IllegalArgumentException(
                    "Transfer amount is too large."
            );
        }

        if (receiverAccountNumber == null
                || receiverAccountNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Receiver account number is required."
            );
        }

        // Find sender account
        Account sender = accountRepository
                .findByUser_Id(senderUserId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Your bank account was not found."
                        ));

        // Find receiver account
        Account receiver = accountRepository
                .findByAccountNumber(receiverAccountNumber.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Receiver account was not found."
                        ));

        // Prevent transfer to own account
        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException(
                    "You cannot transfer money to your own account."
            );
        }

        // Lock accounts in consistent ID order
        Account first;
        Account second;

        if (sender.getId() < receiver.getId()) {
            first = sender;
            second = receiver;
        } else {
            first = receiver;
            second = sender;
        }

        first = accountRepository
                .findByIdForUpdate(first.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bank account was not found."
                        ));

        second = accountRepository
                .findByIdForUpdate(second.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Bank account was not found."
                        ));

        // Restore sender and receiver references
        if (first.getId().equals(sender.getId())) {
            sender = first;
            receiver = second;
        } else {
            receiver = first;
            sender = second;
        }

        // Validate accounts
        validateAccount(sender, "Your");
        validateAccount(receiver, "Receiver's");

        // Check balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance for this transfer."
            );
        }

        // Calculate new balances
        BigDecimal senderBalance =
                sender.getBalance().subtract(amount);

        BigDecimal receiverBalance =
                receiver.getBalance().add(amount);

        if (receiverBalance.precision() > 15) {
            throw new IllegalArgumentException(
                    "Receiver account balance limit exceeded."
            );
        }

        // Update balances
        sender.setBalance(senderBalance);
        receiver.setBalance(receiverBalance);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Generate a numeric reference number
        // Repository must contain existsByReferenceNumber()
        String reference;

        do {
            reference = String.format(
                    "%015d",
                    random.nextLong(1_000_000_000_000_000L)
            );
        } while (transactionRepository
                .existsByReferenceNumber(reference));

        // Create transaction record
        Transaction transaction = new Transaction();

        transaction.setReferenceNumber(reference);
        transaction.setTransactionType("TRANSFER");
        transaction.setAmount(amount);
        transaction.setStatus("SUCCESS");

        transaction.setDescription(
                "Transfer from account "
                        + sender.getAccountNumber()
                        + " to account "
                        + receiver.getAccountNumber()
        );

        transaction.setSenderAccount(sender);
        transaction.setReceiverAccount(receiver);

        transactionRepository.save(transaction);

        return reference;
    }

    private void validateAccount(
            Account account,
            String accountOwner) {

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    accountOwner + " account is not active."
            );
        }

        if (account.getVerificationStatus()
                != VerificationStatus.VERIFIED) {
            throw new IllegalArgumentException(
                    accountOwner
                            + " account must be verified before transfer."
            );
        }
    }
}
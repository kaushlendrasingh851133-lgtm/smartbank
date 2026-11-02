package com.smartbank.smartbank.repository;

import com.smartbank.smartbank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    boolean existsByReferenceNumber(String referenceNumber);

    // Check whether a loan already has a linked transaction
    boolean existsByLoan_Id(Long loanId);

    // Find the transaction linked to a particular loan
    java.util.Optional<Transaction> findByLoan_Id(Long loanId);

    // Find transactions for a particular user's account
    List<Transaction>
    findBySenderAccount_IdOrReceiverAccount_IdOrderByCreatedAtDesc(
            Long senderId, Long receiverId
    );

    // Fetch all transactions with sender and receiver accounts
    @Query("""
        SELECT t FROM Transaction t
        LEFT JOIN FETCH t.senderAccount
        LEFT JOIN FETCH t.receiverAccount
        ORDER BY t.createdAt DESC
    """)
    List<Transaction> findAllWithAccounts();
}

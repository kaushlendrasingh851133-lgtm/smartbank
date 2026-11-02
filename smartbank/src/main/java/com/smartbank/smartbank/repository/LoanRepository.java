package com.smartbank.smartbank.repository;

import com.smartbank.smartbank.entity.Loan;
import com.smartbank.smartbank.entity.LoanStatus;
import com.smartbank.smartbank.entity.User;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Customer ki loan applications
    @Query("""
        SELECT l FROM Loan l
        JOIN FETCH l.user
        WHERE l.user = :user
        ORDER BY l.appliedAt DESC
    """)
    List<Loan> findByUserOrderByAppliedAtDesc(
            @Param("user") User user
    );

    // Admin ki saari loan applications
    @Query("""
        SELECT l FROM Loan l
        JOIN FETCH l.user
        ORDER BY l.appliedAt DESC
    """)
    List<Loan> findAllByOrderByAppliedAtDesc();

    // Status ke according count
    long countByStatus(LoanStatus status);

    // Loan with customer details
    @Query("""
        SELECT l FROM Loan l
        JOIN FETCH l.user
        WHERE l.id = :id
    """)
    Optional<Loan> findByIdWithUser(@Param("id") Long id);

    // Loan ko lock karke fetch karna
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT l FROM Loan l
        JOIN FETCH l.user
        WHERE l.id = :id
    """)
    Optional<Loan> findByIdWithUserForUpdate(
            @Param("id") Long id
    );

    // User ke paas particular status ka loan hai ya nahi
    boolean existsByUserAndStatus(
            User user,
            LoanStatus status
    );

    // User ke particular status wale loans ki count
    long countByUserAndStatus(
            User user,
            LoanStatus status
    );
}
package com.smartbank.smartbank.repository;

import com.smartbank.smartbank.entity.Loan;
import com.smartbank.smartbank.entity.LoanRepayment;
import com.smartbank.smartbank.entity.RepaymentStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepaymentRepository
        extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoanOrderByInstallmentNumberAsc(Loan loan);

    // Admin: fetch repayment, loan and customer details.
    @Query("""
        SELECT r
        FROM LoanRepayment r
        JOIN FETCH r.loan l
        JOIN FETCH l.user
        ORDER BY r.dueDate ASC, r.installmentNumber ASC
    """)
    List<LoanRepayment> findAllWithLoanAndUser();

    @Query("""
        SELECT r
        FROM LoanRepayment r
        JOIN FETCH r.loan l
        WHERE l.user.id = :userId
        ORDER BY r.dueDate ASC
    """)
    List<LoanRepayment> findByLoan_User_IdOrderByDueDateAsc(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT r
        FROM LoanRepayment r
        JOIN FETCH r.loan l
        WHERE r.id = :repaymentId
          AND l.user.id = :userId
    """)
    Optional<LoanRepayment> findByIdAndLoan_User_Id(
            @Param("repaymentId") Long repaymentId,
            @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r
        FROM LoanRepayment r
        JOIN FETCH r.loan l
        WHERE r.id = :repaymentId
          AND l.user.id = :userId
    """)
    Optional<LoanRepayment> findByIdAndLoan_User_IdForUpdate(
            @Param("repaymentId") Long repaymentId,
            @Param("userId") Long userId
    );

    boolean existsByLoanAndInstallmentNumber(
            Loan loan,
            Integer installmentNumber
    );

    long countByLoanAndPaymentStatus(
            Loan loan,
            RepaymentStatus paymentStatus
    );

    boolean existsByLoan_User_IdAndPaymentStatusNot(
            Long userId,
            RepaymentStatus paymentStatus
    );

    long countByLoanAndPaymentStatusNot(
            Loan loan,
            RepaymentStatus paymentStatus
    );
}
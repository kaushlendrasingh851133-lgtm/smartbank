package com.smartbank.smartbank.repository;

import com.smartbank.smartbank.entity.FixedDeposit;
import com.smartbank.smartbank.entity.FixedDepositStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface FixedDepositRepository
        extends JpaRepository<FixedDeposit, Long> {

    List<FixedDeposit> findByUser_IdOrderByCreatedAtDesc(Long userId);

    // Admin: fetch FD customer and account details together.
    @Query("""
        SELECT f
        FROM FixedDeposit f
        JOIN FETCH f.user
        JOIN FETCH f.account
        ORDER BY f.createdAt DESC
    """)
    List<FixedDeposit> findAllWithUserAndAccount();

    // Existing maturity-processing query preserved.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT f FROM FixedDeposit f
        WHERE f.status = :status
          AND f.maturityDate <= :today
        ORDER BY f.id
    """)
    List<FixedDeposit> findMaturedForUpdate(
            @Param("status") FixedDepositStatus status,
            @Param("today") LocalDate today
    );
}
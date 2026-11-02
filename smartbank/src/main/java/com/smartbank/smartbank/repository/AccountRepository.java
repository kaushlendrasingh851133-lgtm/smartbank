
package com.smartbank.smartbank.repository;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.VerificationStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository
        extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByUser_Id(Long userId);

    long countByVerificationStatus(VerificationStatus status);

    // Fetch accounts along with customer details
    @Query("""
        SELECT a FROM Account a
        JOIN FETCH a.user
        ORDER BY a.createdAt DESC
    """)
    List<Account> findAllWithUser();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}


package com.smartbank.smartbank.repository;

import com.smartbank.smartbank.entity.HelpRequest;
import com.smartbank.smartbank.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HelpRequestRepository
        extends JpaRepository<HelpRequest, Long> {

    List<HelpRequest> findByUserOrderByCreatedAtDesc(User user);

    @Query("""
        SELECT h
        FROM HelpRequest h
        JOIN FETCH h.user
        ORDER BY h.createdAt DESC
    """)
    List<HelpRequest> findAllWithUser();

    @Query("""
        SELECT h
        FROM HelpRequest h
        JOIN FETCH h.user
        WHERE h.id = :id
    """)
    Optional<HelpRequest> findByIdWithUser(@Param("id") Long id);

    // Delete help requests older than the given time
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}

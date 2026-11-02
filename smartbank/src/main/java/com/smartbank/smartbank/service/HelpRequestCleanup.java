
package com.smartbank.smartbank.service;

import com.smartbank.smartbank.repository.HelpRequestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class HelpRequestCleanup {

    private final HelpRequestRepository helpRequestRepository;

    public HelpRequestCleanup(
            HelpRequestRepository helpRequestRepository) {
        this.helpRequestRepository = helpRequestRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteOldHelpRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        helpRequestRepository.deleteByCreatedAtBefore(cutoff);
    }
}


package com.smartbank.smartbank.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FixedDepositScheduler {

    private final FixedDepositService fixedDepositService;

    public FixedDepositScheduler(
            FixedDepositService fixedDepositService) {
        this.fixedDepositService = fixedDepositService;
    }

    // Runs every day at midnight according to server time.
    @Scheduled(cron = "0 0 0 * * *")
    public void processMaturities() {
        fixedDepositService.processMaturedFDs();
    }
}

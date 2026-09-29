package com.remo.realestatemaintainceoptimizer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically deletes demo accounts whose session ended without an explicit logout, e.g. because the browser tab was closed.
 */
@Component
public class DemoAccountCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoAccountCleanupScheduler.class);

    private final DemoAccountService demoAccountService;

    public DemoAccountCleanupScheduler(DemoAccountService demoAccountService) {
        this.demoAccountService = demoAccountService;
    }

    /**
     * Removes every expired demo account, shortly after startup and then every 15 minutes.
     */
    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
    public void deleteExpiredDemoAccounts() {
        int deletedCount = demoAccountService.deleteExpiredDemoAccounts();
        if (deletedCount > 0) {
            LOGGER.info("Deleted {} expired demo account(s)", deletedCount);
        }
    }
}

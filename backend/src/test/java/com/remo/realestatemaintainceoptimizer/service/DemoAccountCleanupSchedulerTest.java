package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.lang.reflect.Method;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Verifies that the scheduled cleanup removes expired demo accounts only and is actually registered as a recurring job.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DemoAccountCleanupSchedulerTest {

    @Autowired
    private DemoAccountCleanupScheduler scheduler;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearDatabase() {
        userRepository.deleteAllInBatch();
    }

    @Test
    void deletesExpiredDemoAccountsButKeepsActiveOnes() {
        User expired = TestAccounts.saveDemoAccount(userRepository, Instant.now().minusSeconds(1), 3, 3);
        User active = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3);

        scheduler.deleteExpiredDemoAccounts();

        assertThat(userRepository.findById(expired.id())).isEmpty();
        assertThat(userRepository.findById(active.id())).isPresent();
    }

    @Test
    void runsEveryFifteenMinutes() throws NoSuchMethodException {
        Method job = DemoAccountCleanupScheduler.class.getMethod("deleteExpiredDemoAccounts");

        assertThat(job.getAnnotation(Scheduled.class).fixedDelayString()).isEqualTo("PT15M");
    }
}

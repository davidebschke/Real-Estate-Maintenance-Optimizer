package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.security.UsernamePolicy;
import com.remo.realestatemaintainceoptimizer.exception.RateLimitExceededException;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Verifies demo account creation (expiry, creation limits, example data), its per-client and global limits also under concurrency, and the removal of expired demo accounts.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DemoAccountServiceTest {

    @Autowired
    private DemoAccountService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockitoSpyBean
    private DemoDataSeeder demoDataSeeder;

    @BeforeEach
    void clearDatabase() {
        userRepository.deleteAllInBatch();
    }

    @Test
    void createsAPasswordlessDemoAccountThatExpiresAfterOneSession() {
        Instant before = Instant.now();

        User demo = service.createDemoAccount(TestAccounts.uniqueClientAddress());

        assertThat(demo.demoAccount()).isTrue();
        assertThat(demo.username()).startsWith(UsernamePolicy.DEMO_PREFIX);
        assertThat(demo.passwordHash()).isNull();
        assertThat(demo.displayName()).isEqualTo(DemoAccountService.DEMO_DISPLAY_NAME);
        assertThat(Duration.between(demo.createdAt(), demo.expiresAt())).isEqualTo(Duration.ofHours(8));
        assertThat(demo.createdAt()).isAfterOrEqualTo(before.minusMillis(1));
    }

    @Test
    void givesTheDemoAccountThreeMorePropertyAndAppointmentCreations() {
        User demo = service.createDemoAccount(TestAccounts.uniqueClientAddress());

        assertThat(demo.remainingPropertyCreations()).isEqualTo(3);
        assertThat(demo.remainingAppointmentCreations()).isEqualTo(3);
    }

    @Test
    void fillsTheDemoAccountWithFivePropertiesAndThirtyAppointments() {
        User demo = service.createDemoAccount(TestAccounts.uniqueClientAddress());

        assertThat(propertyRepository.findAllByOwnerIdOrderByNameAsc(demo.id())).hasSize(5);
        assertThat(appointmentRepository.findAllByPropertyOwnerIdOrderByStartAsc(demo.id())).hasSize(30);
    }

    @Test
    void everyDemoAccountGetsItsOwnUniqueUsername() {
        String clientAddress = TestAccounts.uniqueClientAddress();

        User first = service.createDemoAccount(clientAddress);
        User second = service.createDemoAccount(clientAddress);

        assertThat(first.username()).isNotEqualTo(second.username());
    }

    @Test
    void rejectsMoreThanTenDemoAccountsPerClientWithinTheWindow() {
        String clientAddress = TestAccounts.uniqueClientAddress();
        IntStream.range(0, 10).forEach(index -> service.createDemoAccount(clientAddress));

        assertThatThrownBy(() -> service.createDemoAccount(clientAddress))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("reasonCode").isEqualTo(RateLimitExceededException.REASON_TOO_MANY_DEMO_ACCOUNTS);
        assertThat(service.createDemoAccount(TestAccounts.uniqueClientAddress())).isNotNull();
    }

    @Test
    void rejectsNewDemoAccountsOnceTheGlobalCapacityIsReached() {
        IntStream.range(0, 100).forEach(index ->
                TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3));

        assertThatThrownBy(() -> service.createDemoAccount(TestAccounts.uniqueClientAddress()))
                .isInstanceOf(RateLimitExceededException.class)
                .extracting("reasonCode").isEqualTo(RateLimitExceededException.REASON_DEMO_CAPACITY_REACHED);
    }

    @Test
    void concurrentCreationsNeverTogetherExceedTheGlobalCapacity() throws InterruptedException {
        IntStream.range(0, 99).forEach(index ->
                TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3));
        int parallelCreations = 6;
        AtomicInteger created = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(parallelCreations)) {
            for (int index = 0; index < parallelCreations; index++) {
                executor.submit(() -> {
                    start.await();
                    try {
                        service.createDemoAccount(TestAccounts.uniqueClientAddress());
                        created.incrementAndGet();
                    } catch (RateLimitExceededException exception) {
                        return null;
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(created.get()).isEqualTo(1);
        assertThat(userRepository.countByDemoAccountTrue()).isEqualTo(100);
    }

    @Test
    void aFailedCreationDoesNotUseUpOneOfTheClientsCreations() {
        String clientAddress = TestAccounts.uniqueClientAddress();
        doThrow(new IllegalStateException("seeding failed")).when(demoDataSeeder).seed(anyString(), any(LocalDate.class));
        IntStream.range(0, 10).forEach(index -> assertThatThrownBy(() -> service.createDemoAccount(clientAddress))
                .isInstanceOf(IllegalStateException.class));
        doCallRealMethod().when(demoDataSeeder).seed(anyString(), any(LocalDate.class));

        assertThat(service.createDemoAccount(clientAddress).demoAccount()).isTrue();
        assertThat(userRepository.countByDemoAccountTrue()).isEqualTo(1);
    }

    @Test
    void deletesOnlyExpiredDemoAccounts() {
        User expired = TestAccounts.saveDemoAccount(userRepository, Instant.now().minusSeconds(1), 3, 3);
        User active = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3);
        User regular = TestAccounts.saveRegularAccount(userRepository);

        int deletedCount = service.deleteExpiredDemoAccounts();

        assertThat(deletedCount).isEqualTo(1);
        assertThat(userRepository.findById(expired.id())).isEmpty();
        assertThat(userRepository.findById(active.id())).isPresent();
        assertThat(userRepository.findById(regular.id())).isPresent();
    }
}

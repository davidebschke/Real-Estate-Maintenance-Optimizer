package com.remo.realestatemaintainceoptimizer.service;

import static com.remo.realestatemaintainceoptimizer.OptimizationFixtures.appointment;
import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.OptimizationFixtures;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.SavingsGranularity;
import com.remo.realestatemaintainceoptimizer.dto.SavingsPeriodResponse;
import com.remo.realestatemaintainceoptimizer.dto.SavingsStatisticsResponse;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationRun;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationProposalRepository;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationRunRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies the savings statistics against a real PostgreSQL database: only accepted proposals of the account count, totals add up and every week or month from the first acceptance to the current one appears, empty ones as zero.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OptimizationSavingsServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    @Autowired
    private OptimizationSavingsService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private OptimizationRunRepository runRepository;

    @Autowired
    private OptimizationProposalRepository proposalRepository;

    private User owner;
    private Appointment appointment;
    private OptimizationRun run;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        Property property = propertyRepository.save(OptimizationFixtures.westProperty(owner.id()));
        appointment = appointmentRepository.save(appointment(property, OptimizationFixtures.firstPlannableTuesday().atTime(8, 0), false));
        run = runRepository.save(new OptimizationRun(UUID.randomUUID().toString(), owner.id(), Instant.now(), "claude-haiku-4-5", 1));
    }

    private void saveProposal(String ownerId, double meters, double seconds, Consumer<OptimizationProposal> decision) {
        OptimizationProposal proposal = new OptimizationProposal(UUID.randomUUID().toString(), run.id(), ownerId, appointment,
                appointment.start().plusDays(1), appointment.end().plusDays(1), meters, seconds, "", Instant.now());
        decision.accept(proposal);
        proposalRepository.save(proposal);
    }

    private void saveAccepted(double meters, double seconds, Instant decidedAt) {
        saveProposal(owner.id(), meters, seconds, proposal -> proposal.accept(meters, seconds, decidedAt));
    }

    @Test
    void anAccountWithoutAcceptedProposalsHasNoSavings() {
        saveProposal(owner.id(), 1_000, 60, proposal -> proposal.reject(Instant.now()));
        saveProposal(owner.id(), 1_000, 60, proposal -> proposal.expire(Instant.now()));
        saveProposal(owner.id(), 1_000, 60, proposal -> { });

        SavingsStatisticsResponse statistics = service.statistics(owner.id(), SavingsGranularity.WEEK);

        assertThat(statistics.granularity()).isEqualTo(SavingsGranularity.WEEK);
        assertThat(statistics.totalSavedDistanceMeters()).isZero();
        assertThat(statistics.totalSavedDurationSeconds()).isZero();
        assertThat(statistics.acceptedProposalCount()).isZero();
        assertThat(statistics.periods()).isEmpty();
    }

    @Test
    void sumsTheAcceptedProposalsPerWeekAndFillsEmptyWeeksUpToTheCurrentOne() {
        Instant twoWeeksAgo = Instant.now().minus(Duration.ofDays(14));
        saveAccepted(4_000, 300, twoWeeksAgo);
        saveAccepted(6_000, 600, twoWeeksAgo);
        saveAccepted(2_500, 120, Instant.now());

        SavingsStatisticsResponse statistics = service.statistics(owner.id(), SavingsGranularity.WEEK);

        LocalDate firstWeek = SavingsGranularity.WEEK.periodStart(twoWeeksAgo.atZone(ZONE).toLocalDate());
        LocalDate currentWeek = SavingsGranularity.WEEK.periodStart(LocalDate.now(ZONE));
        assertThat(statistics.totalSavedDistanceMeters()).isEqualTo(12_500);
        assertThat(statistics.totalSavedDurationSeconds()).isEqualTo(1_020);
        assertThat(statistics.acceptedProposalCount()).isEqualTo(3);
        assertThat(statistics.periods().getFirst()).isEqualTo(new SavingsPeriodResponse(firstWeek, 10_000, 900, 2));
        assertThat(statistics.periods().getLast()).isEqualTo(new SavingsPeriodResponse(currentWeek, 2_500, 120, 1));
        assertThat(statistics.periods()).hasSize(3);
        assertThat(statistics.periods().get(1)).isEqualTo(new SavingsPeriodResponse(firstWeek.plusWeeks(1), 0, 0, 0));
    }

    @Test
    void groupsByMonthAndCountsOnlyTheRequestingAccount() {
        Instant twoMonthsAgo = Instant.now().minus(Duration.ofDays(62));
        saveAccepted(3_000, 240, twoMonthsAgo);
        User otherOwner = TestAccounts.saveRegularAccount(userRepository);
        saveProposal(otherOwner.id(), 9_000, 900, proposal -> proposal.accept(9_000, 900, Instant.now()));

        SavingsStatisticsResponse statistics = service.statistics(owner.id(), SavingsGranularity.MONTH);

        LocalDate firstMonth = SavingsGranularity.MONTH.periodStart(twoMonthsAgo.atZone(ZONE).toLocalDate());
        LocalDate currentMonth = SavingsGranularity.MONTH.periodStart(LocalDate.now(ZONE));
        assertThat(statistics.totalSavedDistanceMeters()).isEqualTo(3_000);
        assertThat(statistics.periods().getFirst()).isEqualTo(new SavingsPeriodResponse(firstMonth, 3_000, 240, 1));
        assertThat(statistics.periods().getLast().periodStart()).isEqualTo(currentMonth);
        assertThat(statistics.periods()).extracting(SavingsPeriodResponse::periodStart)
                .isSorted()
                .doesNotHaveDuplicates();
    }
}

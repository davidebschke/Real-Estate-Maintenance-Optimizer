package com.remo.realestatemaintainceoptimizer.repository;

import static com.remo.realestatemaintainceoptimizer.OptimizationFixtures.appointment;
import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.OptimizationFixtures;
import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationRun;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.entity.User;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

/**
 * Verifies that optimization runs and proposals round-trip through the Flyway-managed PostgreSQL schema, are found only for their account, outlive a deleted appointment for the statistics and disappear with their account.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OptimizationProposalRepositoryTest {

    private static final LocalDateTime START = OptimizationFixtures.firstPlannableTuesday().atTime(10, 0);

    @Autowired
    private OptimizationProposalRepository proposalRepository;

    @Autowired
    private OptimizationRunRepository runRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User owner;
    private Appointment appointment;
    private OptimizationRun run;

    @BeforeEach
    void seed() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        Property property = propertyRepository.save(OptimizationFixtures.eastProperty(owner.id()));
        appointment = appointmentRepository.save(appointment(property, START, false));
        run = runRepository.save(new OptimizationRun(
                UUID.randomUUID().toString(), owner.id(), Instant.now().truncatedTo(ChronoUnit.MICROS), "claude-haiku-4-5", 3));
    }

    private OptimizationProposal saveProposal(String ownerId, LocalDateTime proposedStart) {
        return proposalRepository.save(new OptimizationProposal(UUID.randomUUID().toString(), run.id(), ownerId, appointment,
                proposedStart, proposedStart.plusHours(1), 10_000.0, 900.0, "Spart Fahrzeit.",
                Instant.now().truncatedTo(ChronoUnit.MICROS)));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void persistsAndReloadsAProposalWithTheAppointmentSnapshot() {
        OptimizationProposal saved = saveProposal(owner.id(), START.plusDays(1));
        flushAndClear();

        OptimizationProposal reloaded = proposalRepository.findByIdAndOwnerId(saved.id(), owner.id()).orElseThrow();

        assertThat(reloaded.runId()).isEqualTo(run.id());
        assertThat(reloaded.appointmentId()).isEqualTo(appointment.id());
        assertThat(reloaded.appointmentTitle()).isEqualTo("Heizungswartung");
        assertThat(reloaded.propertyName()).isEqualTo("Rheinhaus Ost");
        assertThat(reloaded.originalStart()).isEqualTo(START);
        assertThat(reloaded.originalEnd()).isEqualTo(START.plusHours(1));
        assertThat(reloaded.proposedStart()).isEqualTo(START.plusDays(1));
        assertThat(reloaded.savedDistanceMeters()).isEqualTo(10_000.0);
        assertThat(reloaded.status()).isEqualTo(OptimizationProposalStatus.PENDING);
        assertThat(reloaded.decidedAt()).isNull();
        assertThat(runRepository.findById(run.id())).hasValueSatisfying(stored -> {
            assertThat(stored.model()).isEqualTo("claude-haiku-4-5");
            assertThat(stored.candidateCount()).isEqualTo(3);
            assertThat(stored.ownerId()).isEqualTo(owner.id());
        });
    }

    @Test
    void findsProposalsOnlyForTheirAccountAndSortedByStateAndTime() {
        OptimizationProposal later = saveProposal(owner.id(), START.plusDays(3));
        OptimizationProposal earlier = saveProposal(owner.id(), START.plusDays(1));
        OptimizationProposal accepted = saveProposal(owner.id(), START.plusDays(2));
        accepted.accept(5_000.0, 300.0, Instant.now().truncatedTo(ChronoUnit.MICROS));
        User otherOwner = TestAccounts.saveRegularAccount(userRepository);
        flushAndClear();

        assertThat(proposalRepository.findAllByOwnerIdAndStatusOrderByProposedStartAsc(owner.id(), OptimizationProposalStatus.PENDING))
                .extracting(OptimizationProposal::id)
                .containsExactly(earlier.id(), later.id());
        assertThat(proposalRepository.findAllByOwnerIdAndStatusOrderByDecidedAtAsc(owner.id(), OptimizationProposalStatus.ACCEPTED))
                .singleElement()
                .satisfies(proposal -> assertThat(proposal.savedDistanceMeters()).isEqualTo(5_000.0));
        assertThat(proposalRepository.findByIdAndOwnerId(earlier.id(), otherOwner.id())).isEmpty();
    }

    @Test
    void keepsAProposalForTheStatisticsWhenItsAppointmentIsDeleted() {
        OptimizationProposal saved = saveProposal(owner.id(), START.plusDays(1));
        flushAndClear();

        appointmentRepository.deleteById(appointment.id());
        flushAndClear();

        assertThat(proposalRepository.findById(saved.id())).hasValueSatisfying(proposal -> {
            assertThat(proposal.appointmentId()).isNull();
            assertThat(proposal.appointmentTitle()).isEqualTo("Heizungswartung");
        });
    }

    @Test
    void deletingTheAccountRemovesItsRunsAndProposals() {
        saveProposal(owner.id(), START.plusDays(1));
        flushAndClear();

        userRepository.deleteAllInBatch();

        assertThat(proposalRepository.count()).isZero();
        assertThat(runRepository.count()).isZero();
    }

    @Test
    void storesTheStartARecurringOccurrenceWasOriginallyPlannedFor() {
        Appointment stored = appointmentRepository.findById(appointment.id()).orElseThrow();
        stored.anchorRecurrenceStart();
        flushAndClear();

        assertThat(appointmentRepository.findById(appointment.id()).orElseThrow().recurrenceAnchor()).isEqualTo(START);
    }
}

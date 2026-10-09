package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.PropertyResponse;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies that the demo example data consists of five located properties, thirty appointments spread over three weeks and twelve appointments inside the AI optimization window, all owned by the seeded account.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DemoDataSeederTest {

    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 9, 28);

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserRepository userRepository;

    private User owner;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveDemoAccount(userRepository, Instant.now().plusSeconds(3600), 3, 3);
    }

    @Test
    void createsFivePropertiesWithStoredCoordinates() {
        seeder.seed(owner.id(), FIRST_DAY);

        List<PropertyResponse> properties = propertyService.listAll(owner.id());
        assertThat(properties).hasSize(5);
        assertThat(properties).allSatisfy(property -> {
            assertThat(property.latitude()).isNotNull();
            assertThat(property.longitude()).isNotNull();
            assertThat(property.icon()).isEqualTo(PropertyService.DEFAULT_ICON);
        });
    }

    @Test
    void spreadsThirtyAppointmentsOverTheThreeWeeksStartingAtTheFirstDay() {
        seeder.seed(owner.id(), FIRST_DAY);

        List<AppointmentResponse> appointments = appointmentService.listAll(owner.id()).stream()
                .filter(appointment -> appointment.start().toLocalDate().isBefore(FIRST_DAY.plusDays(21)))
                .toList();
        assertThat(appointments).hasSize(30);
        assertThat(appointments.getFirst().start().toLocalDate()).isEqualTo(FIRST_DAY);
        assertThat(appointments.getLast().start().toLocalDate()).isBefore(FIRST_DAY.plusDays(21));
        assertThat(appointments).allSatisfy(appointment -> {
            assertThat(appointment.end()).isAfter(appointment.start());
            assertThat(appointment.completed()).isFalse();
            assertThat(appointment.history()).hasSize(1);
        });
    }

    @Test
    void neverSchedulesTwoAppointmentsAtTheSameTime() {
        seeder.seed(owner.id(), FIRST_DAY);

        Set<?> distinctStarts = appointmentService.listAll(owner.id()).stream()
                .map(AppointmentResponse::start)
                .collect(Collectors.toSet());
        assertThat(distinctStarts).hasSize(42);
    }

    @Test
    void addsTwelveAppointmentsOnSixWorkingDaysInsideTheOptimizationWindowPairingDifferentProperties() {
        seeder.seed(owner.id(), FIRST_DAY);

        List<AppointmentResponse> optimizable = appointmentService.listAll(owner.id()).stream()
                .filter(appointment -> !appointment.start().toLocalDate().isBefore(FIRST_DAY.plusDays(28)))
                .toList();
        assertThat(optimizable).hasSize(12);
        assertThat(optimizable).allSatisfy(appointment ->
                assertThat(appointment.start().getDayOfWeek()).isNotEqualTo(DayOfWeek.SUNDAY));
        Map<LocalDate, List<AppointmentResponse>> byDay = optimizable.stream()
                .collect(Collectors.groupingBy(appointment -> appointment.start().toLocalDate()));
        assertThat(byDay).hasSize(6);
        assertThat(byDay.values()).allSatisfy(dayAppointments -> assertThat(
                dayAppointments.stream().map(AppointmentResponse::propertyId).distinct()).hasSize(2));
    }

    @Test
    void usesEveryPropertyAndIncludesLockedAppointments() {
        seeder.seed(owner.id(), FIRST_DAY);

        List<AppointmentResponse> appointments = appointmentService.listAll(owner.id());
        assertThat(appointments.stream().map(AppointmentResponse::propertyId).distinct()).hasSize(5);
        assertThat(appointments).anyMatch(AppointmentResponse::locked);
    }
}

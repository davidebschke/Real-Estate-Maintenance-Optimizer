package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.remo.realestatemaintainceoptimizer.TestAccounts;
import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import com.remo.realestatemaintainceoptimizer.ai.OptimizationAdvisor;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationAvailabilityResponse;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationUnavailableReason;
import com.remo.realestatemaintainceoptimizer.entity.User;
import com.remo.realestatemaintainceoptimizer.exception.RoutingDisabledException;
import com.remo.realestatemaintainceoptimizer.repository.UserRepository;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifies that the AI optimization is reported as unavailable and refuses to start while road routing is switched off, even though the AI itself is on, since its travel matrix needs routing.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OptimizationServiceRoutingDisabledTest {

    @Autowired
    private OptimizationService service;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private OptimizationAdvisor advisor;

    private User owner;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        owner = TestAccounts.saveRegularAccount(userRepository);
        when(advisor.isEnabled()).thenReturn(true);
    }

    @Test
    void reportsRoutingAsTheReasonWhileTheAiIsOn() {
        assertThat(service.availability())
                .isEqualTo(new OptimizationAvailabilityResponse(false, OptimizationUnavailableReason.ROUTING_DISABLED));
    }

    @Test
    void reportsTheAiAsTheReasonWhileBothAreOff() {
        when(advisor.isEnabled()).thenReturn(false);

        assertThat(service.availability())
                .isEqualTo(new OptimizationAvailabilityResponse(false, OptimizationUnavailableReason.AI_DISABLED));
    }

    @Test
    void refusesARunWithoutConsultingTheAi() {
        assertThatThrownBy(() -> service.run(owner.id(), Locale.GERMAN)).isInstanceOf(RoutingDisabledException.class);

        verify(advisor, never()).selectMoves(anyList(), any());
    }
}

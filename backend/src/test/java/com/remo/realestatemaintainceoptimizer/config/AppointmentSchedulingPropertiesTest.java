package com.remo.realestatemaintainceoptimizer.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.remo.realestatemaintainceoptimizer.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifies that {@code remo.appointments.*} properties bind from application.yml and that a negative buffer or one longer than a day is rejected.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AppointmentSchedulingPropertiesTest {

    @Autowired
    private AppointmentSchedulingProperties schedulingProperties;

    @Test
    void bindsTheConfiguredBufferMinutes() {
        assertThat(schedulingProperties.bufferMinutes()).isEqualTo(15);
    }

    @Test
    void allowsAZeroBufferToDisableTheMinimumGap() {
        assertThat(new AppointmentSchedulingProperties(0).bufferMinutes()).isZero();
    }

    @Test
    void rejectsANegativeBuffer() {
        assertThatThrownBy(() -> new AppointmentSchedulingProperties(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsABufferLongerThanADay() {
        assertThat(new AppointmentSchedulingProperties(1440).bufferMinutes()).isEqualTo(1440);
        assertThatThrownBy(() -> new AppointmentSchedulingProperties(1441)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAccountsOwnBufferTakesPrecedenceOverTheConfiguredDefault() {
        AppointmentSchedulingProperties properties = new AppointmentSchedulingProperties(15);

        assertThat(properties.bufferMinutesFor(30)).isEqualTo(30);
        assertThat(properties.bufferMinutesFor(0)).isZero();
        assertThat(properties.bufferMinutesFor(null)).isEqualTo(15);
    }
}

package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.service.TravelMatrix.Travel;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies lookups in the travel matrix: known pairs, the same location, unknown locations, unreachable pairs and the arithmetic of trips.
 */
class TravelMatrixTest {

    private static final TravelMatrix MATRIX = new TravelMatrix(
            List.of("A", "B", "C"),
            List.of(Arrays.asList(0.0, 1_000.0, null), Arrays.asList(1_100.0, 0.0, 2_000.0), Arrays.asList(null, 2_100.0, 0.0)),
            List.of(Arrays.asList(0.0, 120.0, null), Arrays.asList(130.0, 0.0, 240.0), Arrays.asList(null, 250.0, 0.0)));

    @Test
    void returnsTheDirectedTravelBetweenTwoKnownLocations() {
        assertThat(MATRIX.between("A", "B")).contains(new Travel(1_000.0, 120.0));
        assertThat(MATRIX.between("B", "A")).contains(new Travel(1_100.0, 130.0));
    }

    @Test
    void travelWithinTheSameKnownLocationIsFree() {
        assertThat(MATRIX.between("C", "C")).contains(Travel.NONE);
    }

    @Test
    void returnsNothingForAnUnknownLocationOrAnUnreachablePair() {
        assertThat(MATRIX.contains("D")).isFalse();
        assertThat(MATRIX.between("A", "D")).isEmpty();
        assertThat(MATRIX.between("D", "D")).isEmpty();
        assertThat(MATRIX.between("A", "C")).isEmpty();
    }

    @Test
    void aMatrixWithoutTravelKnowsEveryLocationAtZeroCost() {
        TravelMatrix single = TravelMatrix.withoutTravel(List.of("A"));

        assertThat(single.contains("A")).isTrue();
        assertThat(single.between("A", "A")).contains(Travel.NONE);
    }

    @Test
    void addsAndSubtractsTrips() {
        Travel first = new Travel(1_000.0, 100.0);
        Travel second = new Travel(400.0, 40.0);

        assertThat(first.plus(second)).isEqualTo(new Travel(1_400.0, 140.0));
        assertThat(first.minus(second)).isEqualTo(new Travel(600.0, 60.0));
    }
}

package com.remo.realestatemaintainceoptimizer.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Verifies the optimization rules on a small hand-calculated schedule: savings per move, locked and completed appointments, the lead time and horizon of the planning window, the two-week bound of recurring occurrences, working hours, Sundays, buffer plus driving time to the neighbouring appointments and the candidate limits.
 */
class OptimizationPlannerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate DAY_ONE = LocalDate.of(2026, 11, 3);
    private static final LocalDate DAY_TWO = LocalDate.of(2026, 11, 4);
    private static final int BUFFER_MINUTES = 5;
    private static final TravelMatrix MATRIX = new TravelMatrix(
            List.of("A", "B", "C"),
            List.of(List.of(0.0, 10_000.0, 12_000.0), List.of(10_000.0, 0.0, 4_000.0), List.of(12_000.0, 4_000.0, 0.0)),
            List.of(List.of(0.0, 900.0, 1_080.0), List.of(900.0, 0.0, 360.0), List.of(1_080.0, 360.0, 0.0)));
    private static final PlanningContext CONTEXT = new PlanningContext(MATRIX, BUFFER_MINUTES, TODAY);

    private final OptimizationPlanner planner = new OptimizationPlanner(properties(3, 60));

    private static OptimizationProperties properties(int maxCandidatesPerAppointment, int maxCandidatesForAi) {
        return new OptimizationProperties(28, 365, 14, LocalTime.of(7, 0), LocalTime.of(19, 0), 15, 500,
                maxCandidatesPerAppointment, maxCandidatesForAi, 50, 400, ZoneId.of("Europe/Berlin"));
    }

    private static PlanningVisit visit(String id, String locationId, LocalDateTime start, int minutes) {
        return new PlanningVisit(id, locationId, start, start.plusMinutes(minutes), true, false, start);
    }

    private static PlanningVisit fixedVisit(String id, String locationId, LocalDateTime start, int minutes) {
        return new PlanningVisit(id, locationId, start, start.plusMinutes(minutes), false, false, start);
    }

    private static PlanningVisit recurringVisit(String id, String locationId, LocalDateTime start, LocalDateTime anchor) {
        return new PlanningVisit(id, locationId, start, start.plusMinutes(60), true, true, anchor);
    }

    /** Day one visits A and then B, day two only B: moving day one's B next to day two's B saves the A-B leg. */
    private static List<PlanningVisit> splitSchedule(PlanningVisit movedVisit) {
        return List.of(
                fixedVisit("a-day-one", "A", DAY_ONE.atTime(8, 0), 60),
                movedVisit,
                fixedVisit("b-day-two", "B", DAY_TWO.atTime(8, 0), 60));
    }

    @Test
    void movesAVisitNextToAnotherVisitAtTheSameLocationAndReportsTheSavedDistanceAndTime() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);

        List<MoveOption> options = planner.findMoveOptions(splitSchedule(movable), CONTEXT);

        assertThat(options).singleElement().satisfies(option -> {
            assertThat(option.visit().appointmentId()).isEqualTo("b-day-one");
            assertThat(option.newStart()).isEqualTo(DAY_TWO.atTime(9, 15));
            assertThat(option.newEnd()).isEqualTo(DAY_TWO.atTime(10, 15));
            assertThat(option.savedDistanceMeters()).isEqualTo(10_000.0);
            assertThat(option.savedDurationSeconds()).isEqualTo(900.0);
            assertThat(option.durationMinutes()).isEqualTo(60);
        });
    }

    @Test
    void neverMovesALockedOrCompletedVisit() {
        PlanningVisit locked = fixedVisit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);

        assertThat(planner.findMoveOptions(splitSchedule(locked), CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(splitSchedule(locked), locked, DAY_TWO.atTime(9, 15), CONTEXT)).isEmpty();
    }

    @Test
    void neverMovesAVisitWhoseLocationIsUnknown() {
        PlanningVisit unlocated = visit("b-day-one", null, DAY_ONE.atTime(10, 0), 60);

        assertThat(planner.findMoveOptions(splitSchedule(unlocated), CONTEXT)).isEmpty();
    }

    @Test
    void neverPlansADayWithAnAppointmentWhoseLocationIsUnknown() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> unknownStopOnTargetDay = new ArrayList<>(splitSchedule(movable));
        unknownStopOnTargetDay.add(fixedVisit("unknown-day-two", null, DAY_TWO.atTime(12, 0), 60));
        List<PlanningVisit> unknownStopOnOriginDay = new ArrayList<>(splitSchedule(movable));
        unknownStopOnOriginDay.add(fixedVisit("unknown-day-one", null, DAY_ONE.atTime(14, 0), 60));

        assertThat(planner.evaluateMove(unknownStopOnTargetDay, movable, DAY_TWO.atTime(9, 15), CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(unknownStopOnOriginDay, movable, DAY_TWO.atTime(9, 15), CONTEXT)).isEmpty();
        assertThat(planner.findMoveOptions(unknownStopOnTargetDay, CONTEXT)).isEmpty();
    }

    @Test
    void keepsTheBufferToAnAppointmentRunningPastMidnightIntoTheTargetDay() {
        LocalDate dayThree = DAY_TWO.plusDays(1);
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> schedule = new ArrayList<>(splitSchedule(movable));
        schedule.add(fixedVisit("b-day-three", "B", dayThree.atTime(12, 0), 60));
        List<PlanningVisit> withOvernightVisit = new ArrayList<>(schedule);
        withOvernightVisit.add(fixedVisit("overnight", "B", DAY_TWO.atTime(20, 0), 11 * 60 + 50));

        assertThat(planner.evaluateMove(schedule, movable, dayThree.atTime(7, 0), CONTEXT)).isPresent();
        assertThat(planner.evaluateMove(withOvernightVisit, movable, dayThree.atTime(7, 0), CONTEXT)).isEmpty();
    }

    @Test
    void leavesVisitsBeforeTheLeadTimeUntouchedAndNeverMovesIntoThatPeriod() {
        LocalDate tooSoon = TODAY.plusDays(26);
        PlanningVisit soonVisit = visit("b-soon", "B", tooSoon.atTime(10, 0), 60);
        List<PlanningVisit> schedule = List.of(
                fixedVisit("a-soon", "A", tooSoon.atTime(8, 0), 60),
                soonVisit,
                fixedVisit("b-day-two", "B", DAY_TWO.atTime(8, 0), 60));
        PlanningVisit laterVisit = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> intoTheLeadTime = List.of(
                fixedVisit("b-soon", "B", tooSoon.atTime(8, 0), 60),
                fixedVisit("a-day-one", "A", DAY_ONE.atTime(8, 0), 60),
                laterVisit);

        assertThat(planner.findMoveOptions(schedule, CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(intoTheLeadTime, laterVisit, tooSoon.atTime(9, 15), CONTEXT)).isEmpty();
    }

    @Test
    void theWindowStartsAfterTheLeadTimeAndEndsAtTheHorizon() {
        assertThat(planner.isInWindow(TODAY.plusDays(27), TODAY)).isFalse();
        assertThat(planner.isInWindow(TODAY.plusDays(28), TODAY)).isTrue();
        assertThat(planner.isInWindow(TODAY.plusDays(365), TODAY)).isTrue();
        assertThat(planner.isInWindow(TODAY.plusDays(366), TODAY)).isFalse();
    }

    @Test
    void movesARecurringOccurrenceAtMostFourteenDaysAwayFromItsOriginallyPlannedDate() {
        LocalDateTime anchor = DAY_ONE.atTime(10, 0);
        PlanningVisit occurrence = recurringVisit("b-recurring", "B", anchor, anchor);
        LocalDate fourteenDaysLater = DAY_ONE.plusDays(14);
        LocalDate fifteenDaysLater = DAY_ONE.plusDays(15);
        List<PlanningVisit> schedule = List.of(
                fixedVisit("a-day-one", "A", DAY_ONE.atTime(8, 0), 60),
                occurrence,
                fixedVisit("b-fourteen", "B", fourteenDaysLater.atTime(8, 0), 60),
                fixedVisit("b-fifteen", "B", fifteenDaysLater.atTime(8, 0), 60));

        assertThat(planner.evaluateMove(schedule, occurrence, fourteenDaysLater.atTime(9, 15), CONTEXT)).isPresent();
        assertThat(planner.evaluateMove(schedule, occurrence, fifteenDaysLater.atTime(9, 15), CONTEXT)).isEmpty();
    }

    @Test
    void boundsARecurringOccurrenceByItsOriginalDateEvenAfterAnEarlierAutomaticMove() {
        LocalDateTime anchor = DAY_ONE.atTime(10, 0);
        LocalDateTime alreadyMoved = DAY_ONE.plusDays(7).atTime(10, 0);
        PlanningVisit occurrence = recurringVisit("b-recurring", "B", alreadyMoved, anchor);
        LocalDate target = DAY_ONE.plusDays(15);
        List<PlanningVisit> schedule = List.of(
                fixedVisit("a-moved-day", "A", alreadyMoved.toLocalDate().atTime(8, 0), 60),
                occurrence,
                fixedVisit("b-target", "B", target.atTime(8, 0), 60));

        assertThat(planner.evaluateMove(schedule, occurrence, target.atTime(9, 15), CONTEXT)).isEmpty();
    }

    @Test
    void requiresTheDrivingTimeToTheNextAppointmentOnTopOfTheBuffer() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> drivingTimeFits = new ArrayList<>(splitSchedule(movable));
        drivingTimeFits.add(fixedVisit("c-day-two", "C", DAY_TWO.atTime(10, 16), 60));
        List<PlanningVisit> onlyBufferFits = new ArrayList<>(splitSchedule(movable));
        onlyBufferFits.add(fixedVisit("c-day-two", "C", DAY_TWO.atTime(10, 12), 60));

        assertThat(planner.evaluateMove(drivingTimeFits, movable, DAY_TWO.atTime(9, 5), CONTEXT)).isPresent();
        assertThat(planner.evaluateMove(onlyBufferFits, movable, DAY_TWO.atTime(9, 5), CONTEXT)).isEmpty();
    }

    @Test
    void requiresTheDrivingTimeFromThePreviousAppointment() {
        PlanningVisit movable = visit("a-day-one", "A", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> schedule = List.of(
                fixedVisit("b-day-one", "B", DAY_ONE.atTime(8, 0), 60),
                movable,
                fixedVisit("c-day-two", "C", DAY_TWO.atTime(8, 0), 60),
                fixedVisit("a-day-two", "A", DAY_TWO.atTime(12, 0), 60));

        assertThat(planner.evaluateMove(schedule, movable, DAY_TWO.atTime(9, 10), CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(schedule, movable, DAY_TWO.atTime(9, 23), CONTEXT)).isPresent();
    }

    @Test
    void keepsTheBufferToEveryOtherAppointment() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);

        assertThat(planner.evaluateMove(splitSchedule(movable), movable, DAY_TWO.atTime(9, 3), CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(splitSchedule(movable), movable, DAY_TWO.atTime(7, 30), CONTEXT)).isEmpty();
    }

    @Test
    void rejectsSlotsOutsideTheWorkingHoursAndOnSundays() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        LocalDate sunday = LocalDate.of(2026, 11, 8);
        List<PlanningVisit> withSundayVisit = new ArrayList<>(splitSchedule(movable));
        withSundayVisit.add(fixedVisit("b-sunday", "B", sunday.atTime(8, 0), 60));

        assertThat(planner.evaluateMove(splitSchedule(movable), movable, DAY_TWO.atTime(18, 30), CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(splitSchedule(movable), movable, DAY_TWO.atTime(6, 0), CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(splitSchedule(movable), movable, DAY_TWO.atTime(18, 0), CONTEXT)).isPresent();
        assertThat(planner.evaluateMove(withSundayVisit, movable, sunday.atTime(9, 15), CONTEXT)).isEmpty();
    }

    @Test
    void rejectsAMoveThatSavesNoDistance() {
        PlanningVisit movable = visit("a-day-one", "A", DAY_ONE.atTime(8, 0), 60);
        List<PlanningVisit> schedule = List.of(
                movable,
                fixedVisit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60),
                fixedVisit("b-day-two", "B", DAY_TWO.atTime(8, 0), 60));

        assertThat(planner.evaluateMove(schedule, movable, DAY_TWO.atTime(9, 30), CONTEXT)).isEmpty();
        assertThat(planner.evaluateMove(schedule, movable, movable.start(), CONTEXT)).isEmpty();
    }

    @Test
    void rejectsAMoveWhoseRouteCannotBeCalculatedBecauseAPairIsUnreachable() {
        TravelMatrix unreachable = new TravelMatrix(
                List.of("A", "B"),
                List.of(Arrays.asList(0.0, null), Arrays.asList(null, 0.0)),
                List.of(Arrays.asList(0.0, null), Arrays.asList(null, 0.0)));
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        PlanningContext context = new PlanningContext(unreachable, BUFFER_MINUTES, TODAY);

        assertThat(planner.findMoveOptions(splitSchedule(movable), context)).isEmpty();
    }

    @Test
    void returnsAtMostTheConfiguredNumberOfOptionsPerVisitAndOverall() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> schedule = new ArrayList<>(splitSchedule(movable));
        schedule.add(fixedVisit("b-day-three", "B", DAY_TWO.plusDays(1).atTime(8, 0), 60));
        PlanningVisit secondMovable = visit("c-day-four", "C", DAY_TWO.plusDays(2).atTime(10, 0), 60);
        schedule.add(fixedVisit("a-day-four", "A", DAY_TWO.plusDays(2).atTime(8, 0), 60));
        schedule.add(secondMovable);

        assertThat(planner.findMoveOptions(schedule, CONTEXT)).hasSizeGreaterThan(2);
        assertThat(new OptimizationPlanner(properties(1, 60)).findMoveOptions(schedule, CONTEXT))
                .extracting(option -> option.visit().appointmentId())
                .containsExactlyInAnyOrder("b-day-one", "c-day-four");
        assertThat(new OptimizationPlanner(properties(3, 1)).findMoveOptions(schedule, CONTEXT)).hasSize(1);
    }

    @Test
    void sortsTheOptionsByTheSavedDrivingTime() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> schedule = new ArrayList<>(splitSchedule(movable));
        PlanningVisit secondMovable = visit("c-day-four", "C", DAY_TWO.plusDays(2).atTime(10, 0), 60);
        schedule.add(fixedVisit("b-day-four", "B", DAY_TWO.plusDays(2).atTime(8, 0), 60));
        schedule.add(secondMovable);
        schedule.add(fixedVisit("c-day-five", "C", DAY_TWO.plusDays(3).atTime(8, 0), 60));

        List<MoveOption> options = planner.findMoveOptions(schedule, CONTEXT);

        assertThat(options).extracting(MoveOption::savedDurationSeconds).isSortedAccordingTo((first, second) -> Double.compare(second, first));
        assertThat(options.getFirst().visit().appointmentId()).isEqualTo("b-day-one");
    }

    @Test
    void appliesAMoveToTheScheduleKeepingEveryOtherVisit() {
        PlanningVisit movable = visit("b-day-one", "B", DAY_ONE.atTime(10, 0), 60);
        List<PlanningVisit> schedule = splitSchedule(movable);
        MoveOption option = planner.evaluateMove(schedule, movable, DAY_TWO.atTime(9, 15), CONTEXT).orElseThrow();

        List<PlanningVisit> moved = planner.applyMove(schedule, option);

        assertThat(moved).hasSize(3);
        Optional<PlanningVisit> movedVisit = moved.stream().filter(visit -> visit.appointmentId().equals("b-day-one")).findFirst();
        assertThat(movedVisit).hasValueSatisfying(visit -> {
            assertThat(visit.start()).isEqualTo(DAY_TWO.atTime(9, 15));
            assertThat(visit.end()).isEqualTo(DAY_TWO.atTime(10, 15));
        });
        assertThat(planner.evaluateMove(moved, movable.movedTo(DAY_TWO.atTime(9, 15)), DAY_TWO.atTime(9, 15), CONTEXT)).isEmpty();
    }
}

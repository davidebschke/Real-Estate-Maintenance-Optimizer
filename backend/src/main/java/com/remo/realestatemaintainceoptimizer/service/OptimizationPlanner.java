package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.service.TravelMatrix.Travel;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Calculates which appointment moves inside the planning window are feasible and how much road distance and driving time they save, without any I/O, so every rule (locked and completed appointments stay put, recurring occurrences move at most a few days, working hours, buffer plus driving time to the neighbouring appointments) is checked in one place for both candidate generation and the re-validation of AI selections and accepted proposals.
 */
@Component
@EnableConfigurationProperties(OptimizationProperties.class)
public class OptimizationPlanner {

    private static final long SECONDS_PER_MINUTE = 60;
    private static final Comparator<MoveOption> BY_SAVINGS_DESCENDING = Comparator
            .comparingDouble(MoveOption::savedDurationSeconds)
            .thenComparingDouble(MoveOption::savedDistanceMeters)
            .reversed();

    private final OptimizationProperties properties;

    public OptimizationPlanner(OptimizationProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns whether the given visit may be moved by the optimization: movable, at a known location and starting inside the planning window.
     */
    public boolean isPlannable(PlanningVisit visit, LocalDate today) {
        return visit.movable() && visit.located() && isInWindow(visit.date(), today);
    }

    /**
     * Returns whether the given day lies inside the planning window that starts the configured lead time after today and ends at the configured horizon.
     */
    public boolean isInWindow(LocalDate day, LocalDate today) {
        return !day.isBefore(today.plusDays(properties.minLeadDays())) && !day.isAfter(today.plusDays(properties.horizonDays()));
    }

    /**
     * Returns the most saving feasible moves of every plannable visit onto days that already have appointments at known locations, at most the configured number per visit and overall, sorted by saved driving time and then distance.
     */
    public List<MoveOption> findMoveOptions(List<PlanningVisit> schedule, PlanningContext context) {
        SortedSet<LocalDate> busyDays = new TreeSet<>();
        schedule.stream()
                .filter(visit -> visit.located() && isInWindow(visit.date(), context.today()))
                .forEach(visit -> busyDays.add(visit.date()));

        List<MoveOption> options = new ArrayList<>();
        for (PlanningVisit visit : schedule) {
            if (!isPlannable(visit, context.today())) {
                continue;
            }
            List<MoveOption> visitOptions = new ArrayList<>();
            for (LocalDate day : busyDays) {
                if (!isAllowedDay(visit, day, context.today())) {
                    continue;
                }
                for (LocalDateTime candidateStart : candidateStarts(schedule, visit, day, context)) {
                    evaluateMove(schedule, visit, candidateStart, context).ifPresent(visitOptions::add);
                }
            }
            visitOptions.stream()
                    .sorted(BY_SAVINGS_DESCENDING)
                    .limit(properties.maxCandidatesPerAppointment())
                    .forEach(options::add);
        }
        return options.stream().sorted(BY_SAVINGS_DESCENDING).limit(properties.maxCandidatesForAi()).toList();
    }

    /**
     * Returns the move of the given visit to the given start if every rule allows it and it saves at least the configured road distance without costing driving time, empty otherwise.
     */
    public Optional<MoveOption> evaluateMove(
            List<PlanningVisit> schedule, PlanningVisit visit, LocalDateTime newStart, PlanningContext context) {
        PlanningVisit moved = visit.movedTo(newStart);
        if (newStart.equals(visit.start())
                || !isPlannable(visit, context.today())
                || !isWithinWorkingHours(moved)
                || !isAllowedDay(visit, newStart.toLocalDate(), context.today())) {
            return Optional.empty();
        }
        List<PlanningVisit> others = schedule.stream()
                .filter(other -> !other.appointmentId().equals(visit.appointmentId()))
                .toList();
        if (!fitsBetweenNeighbours(others, moved, context)) {
            return Optional.empty();
        }

        List<PlanningVisit> rescheduled = new ArrayList<>(others);
        rescheduled.add(moved);
        Set<LocalDate> affectedDays = new HashSet<>(List.of(visit.date(), moved.date()));
        Optional<Travel> costBefore = routeCost(schedule, affectedDays, context.matrix());
        Optional<Travel> costAfter = routeCost(rescheduled, affectedDays, context.matrix());
        if (costBefore.isEmpty() || costAfter.isEmpty()) {
            return Optional.empty();
        }
        Travel saved = costBefore.get().minus(costAfter.get());
        if (saved.distanceMeters() <= 0
                || saved.distanceMeters() < properties.minSavedDistanceMeters()
                || saved.durationSeconds() < 0) {
            return Optional.empty();
        }
        return Optional.of(new MoveOption(visit, moved.start(), moved.end(), saved.distanceMeters(), saved.durationSeconds()));
    }

    /**
     * Returns the given schedule with the option's visit moved to its new start.
     */
    public List<PlanningVisit> applyMove(List<PlanningVisit> schedule, MoveOption option) {
        return schedule.stream()
                .map(visit -> visit.appointmentId().equals(option.visit().appointmentId())
                        ? visit.movedTo(option.newStart())
                        : visit)
                .toList();
    }

    /**
     * Returns whether the given visit may be placed on the given day: inside the window, not on a Sunday and, for a recurring occurrence, at most the configured number of days away from its originally planned date.
     */
    private boolean isAllowedDay(PlanningVisit visit, LocalDate day, LocalDate today) {
        if (!isInWindow(day, today) || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        return !visit.recurring()
                || Math.abs(ChronoUnit.DAYS.between(visit.recurrenceAnchor().toLocalDate(), day))
                        <= properties.maxRecurringShiftDays();
    }

    private boolean isWithinWorkingHours(PlanningVisit visit) {
        return visit.end().toLocalDate().equals(visit.date())
                && !visit.start().toLocalTime().isBefore(properties.workDayStart())
                && !visit.end().toLocalTime().isAfter(properties.workDayEnd());
    }

    /**
     * Returns the starts worth trying on the given day: the start of the working day, right after each appointment of that day plus buffer and driving time, and just early enough before each of them.
     */
    private SortedSet<LocalDateTime> candidateStarts(
            List<PlanningVisit> schedule, PlanningVisit visit, LocalDate day, PlanningContext context) {
        SortedSet<LocalDateTime> starts = new TreeSet<>();
        starts.add(day.atTime(properties.workDayStart()));
        for (PlanningVisit other : schedule) {
            if (!other.date().equals(day) || other.appointmentId().equals(visit.appointmentId())) {
                continue;
            }
            OptionalLong travelFromOther = travelMinutes(other, visit, context.matrix());
            if (travelFromOther.isPresent()) {
                starts.add(roundUpToSlot(other.end().plusMinutes(context.bufferMinutes() + travelFromOther.getAsLong())));
            }
            OptionalLong travelToOther = travelMinutes(visit, other, context.matrix());
            if (travelToOther.isPresent()) {
                starts.add(roundDownToSlot(other.start()
                        .minusMinutes(context.bufferMinutes() + travelToOther.getAsLong())
                        .minus(visit.duration())));
            }
        }
        return starts;
    }

    /**
     * Returns whether the moved visit keeps the buffer to every other appointment and leaves enough driving time from the appointment before it and to the appointment after it.
     */
    private boolean fitsBetweenNeighbours(List<PlanningVisit> others, PlanningVisit moved, PlanningContext context) {
        int bufferMinutes = context.bufferMinutes();
        PlanningVisit previous = null;
        PlanningVisit next = null;
        for (PlanningVisit other : others) {
            if (other.start().minusMinutes(bufferMinutes).isBefore(moved.end())
                    && moved.start().isBefore(other.end().plusMinutes(bufferMinutes))) {
                return false;
            }
            if (!other.end().isAfter(moved.start())) {
                previous = previous == null || other.end().isAfter(previous.end()) ? other : previous;
            } else {
                next = next == null || other.start().isBefore(next.start()) ? other : next;
            }
        }
        return (previous == null || leavesDrivingTime(previous, moved, context))
                && (next == null || leavesDrivingTime(moved, next, context));
    }

    /**
     * Returns whether the gap between two consecutive visits covers the buffer plus the driving time between them, trusting the buffer alone when a location is unknown.
     */
    private boolean leavesDrivingTime(PlanningVisit earlier, PlanningVisit later, PlanningContext context) {
        if (!earlier.located() || !later.located()) {
            return true;
        }
        OptionalLong travel = travelMinutes(earlier, later, context.matrix());
        if (travel.isEmpty()) {
            return false;
        }
        LocalDateTime earliestStart = earlier.end().plusMinutes(context.bufferMinutes() + travel.getAsLong());
        return !later.start().isBefore(earliestStart);
    }

    /**
     * Returns the driving time in whole minutes (rounded up) between two located visits, empty when either location is unknown or the pair is unreachable.
     */
    private static OptionalLong travelMinutes(PlanningVisit from, PlanningVisit to, TravelMatrix matrix) {
        if (!from.located() || !to.located()) {
            return OptionalLong.empty();
        }
        return matrix.between(from.locationId(), to.locationId())
                .map(travel -> OptionalLong.of((long) Math.ceil(travel.durationSeconds() / SECONDS_PER_MINUTE)))
                .orElse(OptionalLong.empty());
    }

    /**
     * Returns the total road distance and driving time between consecutive located visits of each given day, empty when a pair of them is unreachable.
     */
    private static Optional<Travel> routeCost(List<PlanningVisit> schedule, Set<LocalDate> days, TravelMatrix matrix) {
        Travel total = Travel.NONE;
        for (LocalDate day : days) {
            List<PlanningVisit> dayRoute = schedule.stream()
                    .filter(visit -> visit.located() && visit.date().equals(day))
                    .sorted(Comparator.comparing(PlanningVisit::start))
                    .toList();
            for (int index = 1; index < dayRoute.size(); index++) {
                Optional<Travel> leg = matrix.between(dayRoute.get(index - 1).locationId(), dayRoute.get(index).locationId());
                if (leg.isEmpty()) {
                    return Optional.empty();
                }
                total = total.plus(leg.get());
            }
        }
        return Optional.of(total);
    }

    private LocalDateTime roundUpToSlot(LocalDateTime time) {
        LocalDateTime roundedDown = roundDownToSlot(time);
        return roundedDown.equals(time) ? time : roundedDown.plusMinutes(properties.slotMinutes());
    }

    private LocalDateTime roundDownToSlot(LocalDateTime time) {
        LocalDateTime minuteStart = time.truncatedTo(ChronoUnit.MINUTES);
        long minuteOfDay = minuteStart.getHour() * 60L + minuteStart.getMinute();
        return minuteStart.minusMinutes(minuteOfDay % properties.slotMinutes());
    }
}

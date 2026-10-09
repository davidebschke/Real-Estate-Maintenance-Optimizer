package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.service.DistanceMatrixService.Location;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Turns an account's appointments into the detached planning view the optimization works on, choosing which properties the travel matrix covers.
 */
@Component
@EnableConfigurationProperties(OptimizationProperties.class)
public class PlanningSnapshotFactory {

    private final OptimizationProperties properties;

    public PlanningSnapshotFactory(OptimizationProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the planning view of the given appointments, locating the properties with coordinates that are visited most often on the relevant days (at most the configured matrix size); must run inside a transaction because it reads each appointment's property.
     */
    public PlanningSnapshot snapshot(List<Appointment> appointments, Predicate<LocalDate> isRelevantDay) {
        Map<String, Property> propertiesById = appointments.stream()
                .map(Appointment::property)
                .collect(Collectors.toMap(Property::id, Function.identity(), (first, second) -> first, LinkedHashMap::new));
        Map<String, Long> visitCountByPropertyId = appointments.stream()
                .filter(appointment -> isRelevantDay.test(appointment.start().toLocalDate()))
                .map(Appointment::property)
                .filter(property -> property.latitude() != null && property.longitude() != null)
                .collect(Collectors.groupingBy(Property::id, Collectors.counting()));
        List<Location> locations = visitCountByPropertyId.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(properties.maxMatrixLocations())
                .map(entry -> propertiesById.get(entry.getKey()))
                .map(property -> new Location(property.id(), property.latitude(), property.longitude()))
                .toList();

        Set<String> locatedPropertyIds = locations.stream().map(Location::id).collect(Collectors.toSet());
        List<PlanningVisit> visits = appointments.stream()
                .map(appointment -> PlanningVisit.from(appointment, locatedPropertyIds))
                .toList();
        Map<String, AppointmentLabel> labels = appointments.stream()
                .collect(Collectors.toMap(
                        Appointment::id,
                        appointment -> new AppointmentLabel(appointment.title(), appointment.property().name())));
        return new PlanningSnapshot(visits, locations, labels);
    }

    /**
     * The planning view of an account's appointments together with the locations its travel matrix must cover and the display labels of each appointment.
     */
    public record PlanningSnapshot(List<PlanningVisit> visits, List<Location> locations, Map<String, AppointmentLabel> labels) {
    }

    /**
     * How an appointment is named towards the AI and the user.
     */
    public record AppointmentLabel(String title, String propertyName) {
    }
}

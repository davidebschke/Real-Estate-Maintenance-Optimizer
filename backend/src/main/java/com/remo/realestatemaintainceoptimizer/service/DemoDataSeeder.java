package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.entity.Appointment;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEntry;
import com.remo.realestatemaintainceoptimizer.entity.HistoryEventType;
import com.remo.realestatemaintainceoptimizer.entity.Property;
import com.remo.realestatemaintainceoptimizer.repository.AppointmentRepository;
import com.remo.realestatemaintainceoptimizer.repository.PropertyRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Fills a new demo account with example properties and appointments spread over the upcoming weeks, so every view has something to show.
 */
@Component
public class DemoDataSeeder {

    static final int APPOINTMENT_COUNT = 30;
    static final int SCHEDULE_DAYS = 21;
    static final int OPTIMIZABLE_FIRST_DAY_OFFSET = 35;
    static final int OPTIMIZABLE_DAY_COUNT = 6;
    static final int OPTIMIZABLE_DAY_SPACING = 3;
    static final int OPTIMIZABLE_PROPERTY_STRIDE = 2;

    private static final List<LocalTime> DAILY_START_TIMES =
            List.of(LocalTime.of(8, 0), LocalTime.of(11, 0), LocalTime.of(14, 0));
    private static final List<LocalTime> OPTIMIZABLE_START_TIMES = List.of(LocalTime.of(8, 0), LocalTime.of(14, 0));

    private static final List<PropertyTemplate> PROPERTY_TEMPLATES = List.of(
            new PropertyTemplate("Wohnanlage Sonnenhof", "Aachener Str. 512, 50933 Köln", 50.937634, 6.8922212),
            new PropertyTemplate("Wohnturm Mediapark", "Im Mediapark 8, 50670 Köln", 50.948540, 6.944620),
            new PropertyTemplate("Hofhaus Ehrenfeld", "Venloer Str. 400, 50825 Köln", 50.951170, 6.917180),
            new PropertyTemplate("Quartier Südstadt", "Bonner Str. 180, 50968 Köln", 50.912360, 6.962420),
            new PropertyTemplate("Rheinhaus Deutz", "Deutzer Freiheit 70, 50679 Köln", 50.936150, 6.974960));

    private static final List<AppointmentTemplate> APPOINTMENT_TEMPLATES = List.of(
            new AppointmentTemplate("Heizungswartung", "Brenner prüfen, Druck kontrollieren, Filter reinigen.", 90,
                    false, List.of("Manometer", "Dichtungsset")),
            new AppointmentTemplate("Treppenhausreinigung", "Alle Etagen wischen, Geländer abwischen.", 60,
                    false, List.of("Wischmopp", "Reinigungsmittel")),
            new AppointmentTemplate("Rauchmelderprüfung", "Jährliche Funktionsprüfung aller Rauchmelder.", 45,
                    false, List.of("Prüfspray", "Ersatzbatterien 9V")),
            new AppointmentTemplate("Aufzugsinspektion (TÜV)", "Prüftermin mit dem Sachverständigen.", 120,
                    true, List.of()),
            new AppointmentTemplate("Gartenpflege", "Hecken schneiden, Rasen mähen, Grünschnitt entsorgen.", 120,
                    false, List.of("Heckenschere", "Grünschnittsäcke")),
            new AppointmentTemplate("Wasserschaden begutachten", "Feuchtigkeit im Keller messen und dokumentieren.", 60,
                    false, List.of("Feuchtigkeitsmessgerät")),
            new AppointmentTemplate("Fensterdichtungen erneuern", "Undichte Dichtungen im Erdgeschoss tauschen.", 90,
                    false, List.of("Dichtungsprofil 20 m", "Silikon")),
            new AppointmentTemplate("Dachrinne reinigen", "Laub entfernen und Fallrohre prüfen.", 120,
                    false, List.of("Leiter", "Arbeitshandschuhe")),
            new AppointmentTemplate("Legionellenprüfung", "Wasserproben durch das Labor.", 60,
                    true, List.of("Probenflaschen")),
            new AppointmentTemplate("Türschließer einstellen", "Haustür schließt zu schnell.", 30,
                    false, List.of("Inbusschlüsselsatz")));

    private final PropertyRepository propertyRepository;
    private final AppointmentRepository appointmentRepository;

    public DemoDataSeeder(PropertyRepository propertyRepository, AppointmentRepository appointmentRepository) {
        this.propertyRepository = propertyRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Creates every example property and appointment for the given account, scheduling the appointments over the
     * {@value #SCHEDULE_DAYS} days starting at {@code firstDay} plus a block of deliberately unfavourably spread
     * appointments inside the AI optimization's planning window.
     */
    public void seed(String ownerId, LocalDate firstDay) {
        List<Property> properties = PROPERTY_TEMPLATES.stream()
                .map(template -> propertyRepository.save(new Property(
                        UUID.randomUUID().toString(),
                        ownerId,
                        template.name(),
                        template.address(),
                        PropertyService.DEFAULT_ICON,
                        template.latitude(),
                        template.longitude())))
                .toList();

        Instant createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        List<Appointment> appointments = new ArrayList<>();
        for (int index = 0; index < APPOINTMENT_COUNT; index++) {
            AppointmentTemplate template = APPOINTMENT_TEMPLATES.get(index % APPOINTMENT_TEMPLATES.size());
            LocalDate day = firstDay.plusDays((long) index * SCHEDULE_DAYS / APPOINTMENT_COUNT);
            var start = day.atTime(DAILY_START_TIMES.get(index % DAILY_START_TIMES.size()));
            appointments.add(newAppointment(template, properties.get(index % properties.size()), start, createdAt));
        }
        appointments.addAll(optimizableAppointments(properties, firstDay, createdAt));
        appointmentRepository.saveAll(appointments);
    }

    /**
     * Returns {@value #OPTIMIZABLE_DAY_COUNT} days with two appointments each, starting {@value #OPTIMIZABLE_FIRST_DAY_OFFSET}
     * days after {@code firstDay} and skipping Sundays, that pair far-apart properties on the same day while the same
     * property recurs on other days, so the AI optimization has route savings to find.
     */
    private List<Appointment> optimizableAppointments(List<Property> properties, LocalDate firstDay, Instant createdAt) {
        List<Appointment> appointments = new ArrayList<>();
        LocalDate day = firstDay.plusDays(OPTIMIZABLE_FIRST_DAY_OFFSET);
        for (int dayIndex = 0; dayIndex < OPTIMIZABLE_DAY_COUNT; dayIndex++) {
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                day = day.plusDays(1);
            }
            for (int slot = 0; slot < OPTIMIZABLE_START_TIMES.size(); slot++) {
                int templateIndex = (dayIndex * OPTIMIZABLE_START_TIMES.size() + slot) % APPOINTMENT_TEMPLATES.size();
                Property property = properties.get((dayIndex + slot * OPTIMIZABLE_PROPERTY_STRIDE) % properties.size());
                appointments.add(newAppointment(
                        APPOINTMENT_TEMPLATES.get(templateIndex), property, day.atTime(OPTIMIZABLE_START_TIMES.get(slot)), createdAt));
            }
            day = day.plusDays(OPTIMIZABLE_DAY_SPACING);
        }
        return appointments;
    }

    private static Appointment newAppointment(
            AppointmentTemplate template, Property property, LocalDateTime start, Instant createdAt) {
        return new Appointment(
                UUID.randomUUID().toString(),
                null,
                template.title(),
                property,
                template.description(),
                start,
                start.plusMinutes(template.durationMinutes()),
                template.locked(),
                false,
                null,
                template.materials(),
                List.of(new HistoryEntry(createdAt, HistoryEventType.CREATED, List.of())),
                null);
    }

    private record PropertyTemplate(String name, String address, double latitude, double longitude) {
    }

    private record AppointmentTemplate(
            String title, String description, int durationMinutes, boolean locked, List<String> materials) {
    }
}

package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.AppointmentResponse;
import com.remo.realestatemaintainceoptimizer.dto.CreateAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.dto.MoveAppointmentRequest;
import com.remo.realestatemaintainceoptimizer.service.AppointmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Exposes appointment create, read, move and delete operations to the frontend.
 */
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * Returns every appointment, sorted by start time.
     */
    @GetMapping
    public List<AppointmentResponse> listAppointments() {
        return appointmentService.listAll();
    }

    /**
     * Returns the appointment with the given id.
     */
    @GetMapping("/{id}")
    public AppointmentResponse getAppointment(@PathVariable String id) {
        return appointmentService.getById(id);
    }

    /**
     * Creates a new appointment, returning the first (or only) occurrence.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        AppointmentResponse created = appointmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Reschedules an unlocked appointment.
     */
    @PatchMapping("/{id}/schedule")
    public AppointmentResponse moveAppointment(@PathVariable String id, @Valid @RequestBody MoveAppointmentRequest request) {
        return appointmentService.move(id, request);
    }

    /**
     * Deletes an appointment; {@code scope=series} also deletes every not-yet-past occurrence of its recurring series.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String id, @RequestParam(defaultValue = "single") String scope) {
        appointmentService.delete(id, scope);
        return ResponseEntity.noContent().build();
    }
}

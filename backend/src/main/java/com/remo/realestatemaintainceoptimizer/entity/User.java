package com.remo.realestatemaintainceoptimizer.entity;

import com.remo.realestatemaintainceoptimizer.exception.CreationQuotaExceededException;
import com.remo.realestatemaintainceoptimizer.security.UsernamePolicy;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Locale;

/**
 * An account that owns properties and, through them, appointments, persisted as one row of the {@code users} table.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = UsernamePolicy.MAX_LENGTH)
    private String username;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "demo_account", nullable = false)
    private boolean demoAccount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "remaining_property_creations")
    private Integer remainingPropertyCreations;

    @Column(name = "remaining_appointment_creations")
    private Integer remainingAppointmentCreations;

    @Column(name = "remaining_tenant_creations")
    private Integer remainingTenantCreations;

    @Column(name = "remaining_ai_optimizations")
    private Integer remainingAiOptimizations;

    @Column(name = "sessions_valid_from")
    private Instant sessionsValidFrom;

    @Column(name = "appointment_buffer_minutes")
    private Integer appointmentBufferMinutes;

    @Version
    private Long version;

    protected User() {
    }

    public User(
            String id,
            String username,
            String passwordHash,
            String displayName,
            boolean demoAccount,
            Instant createdAt,
            Instant expiresAt,
            Integer remainingPropertyCreations,
            Integer remainingAppointmentCreations,
            Integer remainingTenantCreations,
            Integer remainingAiOptimizations) {
        this.id = id;
        this.username = normalizeUsername(username);
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.demoAccount = demoAccount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.remainingPropertyCreations = remainingPropertyCreations;
        this.remainingAppointmentCreations = remainingAppointmentCreations;
        this.remainingTenantCreations = remainingTenantCreations;
        this.remainingAiOptimizations = remainingAiOptimizations;
    }

    /**
     * Creates an account with the given property, appointment and tenant limits and no AI optimization limit.
     */
    public User(
            String id,
            String username,
            String passwordHash,
            String displayName,
            boolean demoAccount,
            Instant createdAt,
            Instant expiresAt,
            Integer remainingPropertyCreations,
            Integer remainingAppointmentCreations,
            Integer remainingTenantCreations) {
        this(id, username, passwordHash, displayName, demoAccount, createdAt, expiresAt,
                remainingPropertyCreations, remainingAppointmentCreations, remainingTenantCreations, null);
    }

    /**
     * Creates an account with the given property and appointment limits and no tenant limit.
     */
    public User(
            String id,
            String username,
            String passwordHash,
            String displayName,
            boolean demoAccount,
            Instant createdAt,
            Instant expiresAt,
            Integer remainingPropertyCreations,
            Integer remainingAppointmentCreations) {
        this(id, username, passwordHash, displayName, demoAccount, createdAt, expiresAt,
                remainingPropertyCreations, remainingAppointmentCreations, null);
    }

    /**
     * Creates a regular, never-expiring account without creation limits.
     */
    public static User regular(String id, String username, String passwordHash, String displayName, Instant createdAt) {
        return new User(id, username, passwordHash, displayName, false, createdAt, null, null, null, null, null);
    }

    /**
     * Returns the canonical, case-insensitive form in which usernames are stored and looked up.
     */
    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Returns whether this account is no longer usable at the given instant.
     */
    public boolean isExpiredAt(Instant instant) {
        return expiresAt != null && !expiresAt.isAfter(instant);
    }

    /**
     * Revokes every session of this account started before the given instant, e.g. on logout.
     */
    public void invalidateSessionsStartedBefore(Instant instant) {
        this.sessionsValidFrom = instant;
    }

    /**
     * Returns whether a session started at the given instant is still accepted, i.e. was not revoked afterwards.
     */
    public boolean acceptsSessionStartedAt(Instant sessionStart) {
        return sessionsValidFrom == null || !sessionStart.isBefore(sessionsValidFrom);
    }

    /**
     * Replaces the stored password hash.
     */
    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    /**
     * Replaces the username with the given one, stored in its normalized form.
     */
    public void changeUsername(String newUsername) {
        this.username = normalizeUsername(newUsername);
    }

    /**
     * Sets this account's own minimum gap between appointments, overriding the configured default.
     */
    public void changeAppointmentBufferMinutes(int newAppointmentBufferMinutes) {
        this.appointmentBufferMinutes = newAppointmentBufferMinutes;
    }

    /**
     * Uses up one property creation, rejecting it when this account's limit is already exhausted.
     */
    public void consumePropertyCreation() {
        if (remainingPropertyCreations == null) {
            return;
        }
        if (remainingPropertyCreations <= 0) {
            throw new CreationQuotaExceededException(CreationQuotaExceededException.RESOURCE_PROPERTY);
        }
        remainingPropertyCreations--;
    }

    /**
     * Uses up one appointment creation, rejecting it when this account's limit is already exhausted.
     */
    public void consumeAppointmentCreation() {
        if (remainingAppointmentCreations == null) {
            return;
        }
        if (remainingAppointmentCreations <= 0) {
            throw new CreationQuotaExceededException(CreationQuotaExceededException.RESOURCE_APPOINTMENT);
        }
        remainingAppointmentCreations--;
    }

    /**
     * Uses up one tenant creation, rejecting it when this account's limit is already exhausted.
     */
    public void consumeTenantCreation() {
        if (remainingTenantCreations == null) {
            return;
        }
        if (remainingTenantCreations <= 0) {
            throw new CreationQuotaExceededException(CreationQuotaExceededException.RESOURCE_TENANT);
        }
        remainingTenantCreations--;
    }

    /**
     * Rejects starting an AI optimization run when this account's limit is already exhausted, without using one up.
     */
    public void requireAiOptimizationAvailable() {
        if (remainingAiOptimizations != null && remainingAiOptimizations <= 0) {
            throw new CreationQuotaExceededException(CreationQuotaExceededException.RESOURCE_AI_OPTIMIZATION);
        }
    }

    /**
     * Uses up one AI optimization run, rejecting it when this account's limit is already exhausted.
     */
    public void consumeAiOptimization() {
        if (remainingAiOptimizations == null) {
            return;
        }
        requireAiOptimizationAvailable();
        remainingAiOptimizations--;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String displayName() {
        return displayName;
    }

    public boolean demoAccount() {
        return demoAccount;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Integer appointmentBufferMinutes() {
        return appointmentBufferMinutes;
    }

    public Integer remainingPropertyCreations() {
        return remainingPropertyCreations;
    }

    public Integer remainingAppointmentCreations() {
        return remainingAppointmentCreations;
    }

    public Integer remainingTenantCreations() {
        return remainingTenantCreations;
    }

    public Integer remainingAiOptimizations() {
        return remainingAiOptimizations;
    }
}

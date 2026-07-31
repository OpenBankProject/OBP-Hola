package com.openbankproject.health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The last known result of one service check.
 *
 * Mirrors the snapshot the Portal status page publishes (OBP-Frontend
 * packages/shared/src/lib/health-check/state/HealthCheckState.ts) so both apps can be read by the
 * same tooling. One deliberate difference: Portal spells the counter "conecutiveFailures"; that is
 * a typo in its shipped shape and is not reproduced here.
 */
public class HealthCheckSnapshot {

    public enum Status {
        healthy, unhealthy, unknown
    }

    private final String service;
    private final Status status;
    private final Long responseTimeMs;
    private final String error;
    private final Instant lastChecked;
    private final int consecutiveFailures;
    private final long intervalMs;
    private final Map<String, String> details;
    /** Non-core services degrade the overall status rather than failing it. */
    private final boolean soft;

    public HealthCheckSnapshot(String service, Status status, Long responseTimeMs, String error,
                               Instant lastChecked, int consecutiveFailures, long intervalMs,
                               Map<String, String> details, boolean soft) {
        this.service = service;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.error = error;
        this.lastChecked = lastChecked;
        this.consecutiveFailures = consecutiveFailures;
        this.intervalMs = intervalMs;
        this.details = details == null ? new LinkedHashMap<>() : details;
        this.soft = soft;
    }

    /** A service that has never been checked -- reported as unknown, never as healthy. */
    public static HealthCheckSnapshot unknown(String service, long intervalMs, boolean soft) {
        return new HealthCheckSnapshot(service, Status.unknown, null, null,
                Instant.EPOCH, 0, intervalMs, new LinkedHashMap<>(), soft);
    }

    public HealthCheckSnapshot withResult(Status newStatus, Long responseTimeMs, String error,
                                          Map<String, String> details) {
        int failures;
        if (newStatus == Status.unhealthy) {
            failures = this.consecutiveFailures + 1;
        } else if (newStatus == Status.healthy) {
            failures = 0;
        } else {
            failures = this.consecutiveFailures;
        }
        // Drop the previous error once a service recovers, so a stale message cannot linger
        // next to a healthy badge.
        String effectiveError = newStatus == Status.healthy ? null : error;
        return new HealthCheckSnapshot(service, newStatus, responseTimeMs, effectiveError,
                Instant.now(), failures, intervalMs, details, soft);
    }

    /**
     * A result older than two check intervals says nothing about the present, so it is reported
     * as stale and counted as unknown rather than carried forward as fact.
     */
    public boolean isStale(Instant now) {
        if (status == Status.unknown) {
            return false;
        }
        return now.toEpochMilli() - lastChecked.toEpochMilli() > intervalMs * 2;
    }

    public Status effectiveStatus(Instant now) {
        return isStale(now) ? Status.unknown : status;
    }

    public String getService() {
        return service;
    }

    public Status getStatus() {
        return status;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getError() {
        return error;
    }

    public Instant getLastChecked() {
        return lastChecked;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public long getIntervalMs() {
        return intervalMs;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public boolean isSoft() {
        return soft;
    }
}

package com.openbankproject.health;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rolls the individual snapshots into one overall verdict.
 *
 * The rules are Portal's (OBP-Frontend packages/shared/src/lib/health-check/summarize.ts), kept
 * deliberately free of optimistic bias:
 *   - nothing monitored means we know nothing — unknown, never healthy;
 *   - a stale result proves nothing about the present and counts as unknown;
 *   - healthy requires every service to have a fresh, healthy result;
 *   - a failing soft service (here: Redis, which only backs the request-log widget) yields
 *     partial rather than unhealthy, the same way Portal treats one dead OAuth2 provider
 *     among several.
 */
public class HealthSummary {

    public enum Overall {
        healthy, partial, unhealthy, unknown
    }

    private final Instant timestamp;
    private final Overall overallStatus;
    private final int healthPercentage;
    private final Map<String, HealthCheckSnapshot> services;
    private final int total;
    private final int healthy;
    private final int unhealthy;
    private final int unknown;
    private final int stale;

    private HealthSummary(Instant timestamp, Overall overallStatus, int healthPercentage,
                          Map<String, HealthCheckSnapshot> services,
                          int total, int healthy, int unhealthy, int unknown, int stale) {
        this.timestamp = timestamp;
        this.overallStatus = overallStatus;
        this.healthPercentage = healthPercentage;
        this.services = services;
        this.total = total;
        this.healthy = healthy;
        this.unhealthy = unhealthy;
        this.unknown = unknown;
        this.stale = stale;
    }

    public static HealthSummary of(Map<String, HealthCheckSnapshot> snapshots, Instant now) {
        Map<String, HealthCheckSnapshot> services = new LinkedHashMap<>(snapshots);

        int healthy = 0;
        int unhealthy = 0;
        int unknown = 0;
        int stale = 0;
        boolean coreUnhealthy = false;
        boolean softUnhealthy = false;

        for (HealthCheckSnapshot snapshot : services.values()) {
            if (snapshot.isStale(now)) {
                stale++;
            }
            HealthCheckSnapshot.Status effective = snapshot.effectiveStatus(now);
            switch (effective) {
                case healthy:
                    healthy++;
                    break;
                case unhealthy:
                    unhealthy++;
                    if (snapshot.isSoft()) {
                        softUnhealthy = true;
                    } else {
                        coreUnhealthy = true;
                    }
                    break;
                default:
                    unknown++;
                    break;
            }
        }

        int total = services.size();
        Overall overall;
        if (total == 0) {
            overall = Overall.unknown;
        } else if (coreUnhealthy) {
            overall = Overall.unhealthy;
        } else if (softUnhealthy) {
            overall = Overall.partial;
        } else if (unknown > 0) {
            overall = Overall.unknown;
        } else {
            overall = Overall.healthy;
        }

        int percentage = total > 0 ? Math.round((healthy * 100f) / total) : 0;
        return new HealthSummary(now, overall, percentage, services,
                total, healthy, unhealthy, unknown, stale);
    }

    /**
     * Only a core failure makes the app unready. A degraded soft dependency must not take Hola
     * out of a load balancer -- it can still complete every consent flow without Redis.
     */
    public boolean isServing() {
        return overallStatus != Overall.unhealthy;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Overall getOverallStatus() {
        return overallStatus;
    }

    public int getHealthPercentage() {
        return healthPercentage;
    }

    public Map<String, HealthCheckSnapshot> getServices() {
        return services;
    }

    public int getTotal() {
        return total;
    }

    public int getHealthy() {
        return healthy;
    }

    public int getUnhealthy() {
        return unhealthy;
    }

    public int getUnknown() {
        return unknown;
    }

    public int getStale() {
        return stale;
    }
}

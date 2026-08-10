package com.openbankproject.health;

import com.openbankproject.hydra.auth.VO.WellKnown;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs the service checks in the background and holds the latest result for each.
 *
 * Checks run on a timer rather than when /status is requested, following the Portal status page:
 * the page then renders instantly, every viewer sees the same result, and a monitoring poll can
 * never turn into a burst of calls against the very dependency it is checking. The cost is that a
 * result is up to one interval old, which is why each snapshot carries lastChecked and intervalMs
 * and goes stale on its own after two intervals.
 *
 * What is checked reflects what Hola actually needs: it is a pure client, so without OBP-API there
 * is no data and without OBP-OIDC no consent can be authorised. Redis only backs the on-page
 * request-log widget, so it is registered soft -- reported, but it degrades rather than fails.
 */
@Component
public class HealthCheckRegistry {

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckRegistry.class);

    public static final long CHECK_INTERVAL_MS = 60_000L;
    private static final int PROBE_TIMEOUT_MILLIS = 5_000;

    private static final String OBP_API = "OBP API";
    private static final String OBP_OIDC = "OBP-OIDC";
    private static final String REDIS = "Redis";

    private final Map<String, HealthCheckSnapshot> snapshots = new ConcurrentHashMap<>();

    @Value("${obp.base_url}")
    private String obpBaseUrl;

    @Value("${oauth2.public_url:http://localhost:9000/obp-oidc}/.well-known/openid-configuration")
    private String wellKnownUrl;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * The document ObpOidcConfig fetched once at startup. Every consent flow runs off that
     * snapshot, so /status reports on it rather than only on whether the endpoint answers now.
     */
    @Autowired(required = false)
    private WellKnown openIDConfiguration;

    public HealthCheckRegistry() {
        snapshots.put(OBP_API, HealthCheckSnapshot.unknown(OBP_API, CHECK_INTERVAL_MS, false));
        snapshots.put(OBP_OIDC, HealthCheckSnapshot.unknown(OBP_OIDC, CHECK_INTERVAL_MS, false));
        snapshots.put(REDIS, HealthCheckSnapshot.unknown(REDIS, CHECK_INTERVAL_MS, true));
    }

    public Map<String, HealthCheckSnapshot> getSnapshots() {
        return new LinkedHashMap<>(snapshots);
    }

    /**
     * initialDelay 0 so the first sweep runs at startup and the page is meaningful immediately
     * instead of showing "unknown" for the first minute.
     */
    @Scheduled(initialDelay = 0, fixedDelay = CHECK_INTERVAL_MS)
    public void runChecks() {
        checkObpApi();
        checkOidc();
        checkRedis();
    }

    private void record(String service, HealthCheckSnapshot.Status status, Long ms,
                        String error, Map<String, String> details) {
        snapshots.compute(service, (key, previous) -> {
            HealthCheckSnapshot base = previous != null
                    ? previous
                    : HealthCheckSnapshot.unknown(key, CHECK_INTERVAL_MS, REDIS.equals(key));
            return base.withResult(status, ms, error, details);
        });
    }

    private void checkObpApi() {
        String url = obpBaseUrl + "/obp/v5.1.0/root";
        Map<String, String> details = new LinkedHashMap<>();
        details.put("OBP_BASE_URL", obpBaseUrl);
        HttpProbe probe = probe(url);
        record(OBP_API, probe.status, probe.ms, probe.error, details);
    }

    private void checkOidc() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("wellKnownUrl", wellKnownUrl);
        if (openIDConfiguration != null) {
            details.put("issuer", String.valueOf(openIDConfiguration.getIssuer()));
            details.put("token_endpoint", String.valueOf(openIDConfiguration.getTokenEndpoint()));
            details.put("authorization_endpoint", String.valueOf(openIDConfiguration.getAuthorizationEndpoint()));
        }

        HttpProbe probe = probe(wellKnownUrl);
        if (probe.status != HealthCheckSnapshot.Status.healthy) {
            record(OBP_OIDC, probe.status, probe.ms, probe.error, details);
            return;
        }
        // Reachable now is not the same as loaded at startup: if the fetch in ObpOidcConfig's
        // @PostConstruct failed, Hola is running without an authorization endpoint and no consent
        // flow can start, however healthy the endpoint looks from here.
        if (openIDConfiguration == null || openIDConfiguration.getAuthorizationEndpoint() == null) {
            record(OBP_OIDC, HealthCheckSnapshot.Status.unhealthy, probe.ms,
                    "reachable, but no OIDC configuration was loaded at startup — restart Hola once OBP-OIDC is up",
                    details);
            return;
        }
        record(OBP_OIDC, HealthCheckSnapshot.Status.healthy, probe.ms, null, details);
    }

    private void checkRedis() {
        Map<String, String> details = new LinkedHashMap<>();
        long start = System.currentTimeMillis();
        try {
            String pong = redisTemplate.getRequiredConnectionFactory().getConnection().ping();
            details.put("ping", pong == null ? "" : pong);
            record(REDIS, HealthCheckSnapshot.Status.healthy,
                    System.currentTimeMillis() - start, null, details);
        } catch (Exception e) {
            logger.warn("HealthCheckRegistry says: redis check failed: {}", e.getMessage());
            record(REDIS, HealthCheckSnapshot.Status.unhealthy,
                    System.currentTimeMillis() - start,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), details);
        }
    }

    private static final class HttpProbe {
        final HealthCheckSnapshot.Status status;
        final Long ms;
        final String error;

        HttpProbe(HealthCheckSnapshot.Status status, Long ms, String error) {
            this.status = status;
            this.ms = ms;
            this.error = error;
        }
    }

    /**
     * Uses a plain connection rather than the shared RestTemplate on purpose: that one is wrapped
     * by RequestResponseLogger, so every probe would be written into the Redis-backed log widget
     * and, once a minute forever, bury the request log the widget exists to show.
     */
    private HttpProbe probe(String url) {
        long start = System.currentTimeMillis();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(PROBE_TIMEOUT_MILLIS);
            connection.setReadTimeout(PROBE_TIMEOUT_MILLIS);
            int code = connection.getResponseCode();
            long ms = System.currentTimeMillis() - start;
            if (code >= 200 && code < 400) {
                return new HttpProbe(HealthCheckSnapshot.Status.healthy, ms, null);
            }
            return new HttpProbe(HealthCheckSnapshot.Status.unhealthy, ms, "HTTP " + code + " from " + url);
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - start;
            logger.warn("HealthCheckRegistry says: probe of {} failed: {}", url, e.getMessage());
            return new HttpProbe(HealthCheckSnapshot.Status.unhealthy, ms,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /** Exposed so the status page can show when the next sweep is due. */
    public Instant now() {
        return Instant.now();
    }
}

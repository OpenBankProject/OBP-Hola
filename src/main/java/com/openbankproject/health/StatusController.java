package com.openbankproject.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Properties;

/**
 * The probes OBP-API's discovery page links to for this app.
 *
 * AppsPage.scala builds those links as {public_obp_hola_url}/status and /health, so the paths are
 * fixed by that contract rather than chosen here.
 *
 * The two serve different questions, and conflating them is what makes a restart-happy
 * orchestrator kill a healthy app over a sick dependency:
 *   /health   liveness  — is this process alive and serving HTTP? Nothing else.
 *   /status   readiness — can Hola actually do its job right now? Reports every dependency and
 *                         answers 503 when a core one is down.
 */
@RestController
public class StatusController {

    @Autowired
    private HealthCheckRegistry registry;

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"ok\"}");
    }

    @GetMapping("/status")
    public ResponseEntity<String> status(
            @RequestHeader(value = "Accept", required = false) String accept) {

        Instant now = Instant.now();
        Map<String, HealthCheckSnapshot> snapshots = registry.getSnapshots();
        HealthSummary summary = HealthSummary.of(snapshots, now);

        boolean wantsJson = accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE);
        String body = wantsJson ? json(summary, now) : html(summary, now);

        return ResponseEntity
                .status(summary.isServing() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(wantsJson ? MediaType.APPLICATION_JSON : MediaType.TEXT_HTML)
                .body(body);
    }

    // ------------------------------------------------------------------ JSON

    private static String jsonEscape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private String appVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return version == null ? "unknown" : version;
    }

    /**
     * Written into the jar at build time by git-commit-id-maven-plugin, and read the same way
     * OBP-API's StatusPage reads its own. Absent when built from a source tree with no .git, which
     * is why every accessor falls back to "unknown" rather than failing.
     */
    private static final Properties GIT_PROPERTIES = loadGitProperties();

    private static Properties loadGitProperties() {
        Properties properties = new Properties();
        try (InputStream in = StatusController.class.getResourceAsStream("/git.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (Exception ignored) {
            // A missing or unreadable stamp must never take the status page down.
        }
        return properties;
    }

    private String gitCommit() {
        return GIT_PROPERTIES.getProperty("git.commit.id", "unknown");
    }

    private String gitBranch() {
        return GIT_PROPERTIES.getProperty("git.branch", "unknown");
    }

    private String gitBuildTime() {
        return GIT_PROPERTIES.getProperty("git.build.time", "unknown");
    }

    /**
     * True when the build had uncommitted changes. Without this the commit id is misleading --
     * it names the last commit, not necessarily what was compiled.
     */
    private boolean gitDirty() {
        return Boolean.parseBoolean(GIT_PROPERTIES.getProperty("git.dirty", "false"));
    }

    private long uptimeSeconds() {
        return ManagementFactory.getRuntimeMXBean().getUptime() / 1000L;
    }

    private String json(HealthSummary summary, Instant now) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"timestamp\": \"").append(summary.getTimestamp()).append("\",\n");
        sb.append("  \"app\": \"obp-hola\",\n");
        sb.append("  \"version\": \"").append(jsonEscape(appVersion())).append("\",\n");
        sb.append("  \"git_commit\": \"").append(jsonEscape(gitCommit())).append("\",\n");
        sb.append("  \"git_branch\": \"").append(jsonEscape(gitBranch())).append("\",\n");
        sb.append("  \"git_dirty\": ").append(gitDirty()).append(",\n");
        sb.append("  \"build_time\": \"").append(jsonEscape(gitBuildTime())).append("\",\n");
        sb.append("  \"uptimeSeconds\": ").append(uptimeSeconds()).append(",\n");
        sb.append("  \"overallStatus\": \"").append(summary.getOverallStatus()).append("\",\n");
        sb.append("  \"healthPercentage\": ").append(summary.getHealthPercentage()).append(",\n");
        sb.append("  \"summary\": {\"total\": ").append(summary.getTotal())
          .append(", \"healthy\": ").append(summary.getHealthy())
          .append(", \"unhealthy\": ").append(summary.getUnhealthy())
          .append(", \"unknown\": ").append(summary.getUnknown())
          .append(", \"stale\": ").append(summary.getStale()).append("},\n");
        sb.append("  \"services\": {\n");

        int i = 0;
        for (Map.Entry<String, HealthCheckSnapshot> entry : summary.getServices().entrySet()) {
            HealthCheckSnapshot s = entry.getValue();
            sb.append("    \"").append(jsonEscape(entry.getKey())).append("\": {");
            sb.append("\"service\": \"").append(jsonEscape(s.getService())).append("\"");
            sb.append(", \"status\": \"").append(s.effectiveStatus(now)).append("\"");
            sb.append(", \"stale\": ").append(s.isStale(now));
            sb.append(", \"soft\": ").append(s.isSoft());
            if (s.getResponseTimeMs() != null) {
                sb.append(", \"responseTimeMs\": ").append(s.getResponseTimeMs());
            }
            sb.append(", \"lastChecked\": \"").append(s.getLastChecked()).append("\"");
            sb.append(", \"consecutiveFailures\": ").append(s.getConsecutiveFailures());
            sb.append(", \"intervalMs\": ").append(s.getIntervalMs());
            if (s.getError() != null) {
                sb.append(", \"error\": \"").append(jsonEscape(s.getError())).append("\"");
            }
            if (!s.getDetails().isEmpty()) {
                sb.append(", \"details\": {");
                int j = 0;
                for (Map.Entry<String, String> d : s.getDetails().entrySet()) {
                    sb.append("\"").append(jsonEscape(d.getKey())).append("\": \"")
                      .append(jsonEscape(String.valueOf(d.getValue()))).append("\"");
                    if (++j < s.getDetails().size()) {
                        sb.append(", ");
                    }
                }
                sb.append("}");
            }
            sb.append("}");
            if (++i < summary.getServices().size()) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  }\n}");
        return sb.toString();
    }

    // ------------------------------------------------------------------ HTML

    private static String htmlEscape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String colourOf(HealthCheckSnapshot.Status status) {
        switch (status) {
            case healthy:
                return "#2e7d32";
            case unhealthy:
                return "#c62828";
            default:
                return "#757575";
        }
    }

    private static String colourOf(HealthSummary.Overall overall) {
        switch (overall) {
            case healthy:
                return "#2e7d32";
            case partial:
                return "#ef6c00";
            case unhealthy:
                return "#c62828";
            default:
                return "#757575";
        }
    }

    private String card(HealthCheckSnapshot s, Instant now) {
        HealthCheckSnapshot.Status effective = s.effectiveStatus(now);
        String colour = colourOf(effective);
        String mark = effective == HealthCheckSnapshot.Status.healthy ? "&#10003;"
                : effective == HealthCheckSnapshot.Status.unhealthy ? "&#10007;" : "?";

        StringBuilder sb = new StringBuilder();
        sb.append("  <div class=\"card\" style=\"border-left: 6px solid ").append(colour).append(";\">\n");
        sb.append("    <div class=\"card-head\">\n");
        sb.append("      <span class=\"mark\" style=\"background: ").append(colour).append(";\">").append(mark).append("</span>\n");
        sb.append("      <h3>").append(htmlEscape(s.getService()));
        if (s.isSoft()) {
            sb.append(" <span class=\"soft\">soft dependency</span>");
        }
        sb.append("</h3>\n");
        sb.append("    </div>\n");

        sb.append("    <div class=\"meta\">Status: <b style=\"color: ").append(colour).append(";\">")
          .append(effective).append("</b>");
        if (s.getResponseTimeMs() != null) {
            sb.append("  &nbsp; Response Time: <b>").append(s.getResponseTimeMs()).append("ms</b>");
        }
        sb.append("  &nbsp; Last Checked: <b>");
        sb.append(Instant.EPOCH.equals(s.getLastChecked()) ? "never"
                : htmlEscape(s.getLastChecked().truncatedTo(ChronoUnit.SECONDS).toString()));
        sb.append("</b> (checked every ").append(s.getIntervalMs() / 1000).append("s)");
        if (s.isStale(now)) {
            sb.append("  &nbsp; <b style=\"color:#ef6c00;\">stale &mdash; older than two intervals</b>");
        }
        sb.append("</div>\n");

        if (s.getConsecutiveFailures() > 0) {
            sb.append("    <div class=\"fail\">Consecutive Failures: ")
              .append(s.getConsecutiveFailures()).append("</div>\n");
        }

        for (Map.Entry<String, String> d : s.getDetails().entrySet()) {
            sb.append("    <div class=\"detail\"><code>").append(htmlEscape(d.getKey()))
              .append(":</code> ").append(htmlEscape(String.valueOf(d.getValue()))).append("</div>\n");
        }

        if (s.getError() != null) {
            sb.append("    <div class=\"error\">Error: ").append(htmlEscape(s.getError())).append("</div>\n");
        }

        sb.append("  </div>\n");
        return sb.toString();
    }

    private String html(HealthSummary summary, Instant now) {
        StringBuilder cards = new StringBuilder();
        for (HealthCheckSnapshot s : summary.getServices().values()) {
            cards.append(card(s, now));
        }

        String overallColour = colourOf(summary.getOverallStatus());

        return "<!DOCTYPE html>\n<html>\n<head>\n"
                + "  <meta charset=\"utf-8\">\n"
                // Checks run server-side on a timer, so the page only needs to re-read them.
                + "  <meta http-equiv=\"refresh\" content=\"15\">\n"
                + "  <title>OBP Hola - Status</title>\n"
                + "  <style>\n"
                + "    body { font-family: sans-serif; max-width: 900px; margin: 40px auto; padding: 0 20px; color: #222; }\n"
                + "    h1 { margin-bottom: 4px; }\n"
                + "    .sub { color: #666; margin-bottom: 24px; }\n"
                + "    .summary { background: #f5f5f5; border-radius: 8px; padding: 16px 20px; margin-bottom: 28px;\n"
                + "               display: flex; align-items: center; gap: 28px; flex-wrap: wrap; }\n"
                + "    .overall { font-size: 1.5em; font-weight: bold; }\n"
                + "    .counts { display: flex; gap: 24px; flex-wrap: wrap; }\n"
                + "    .count b { display: block; font-size: 1.4em; }\n"
                + "    .count span { color: #666; font-size: 0.85em; }\n"
                + "    .card { background: #fafafa; border-radius: 6px; padding: 14px 18px; margin-bottom: 14px; }\n"
                + "    .card-head { display: flex; align-items: center; gap: 10px; }\n"
                + "    .card h3 { margin: 0; font-size: 1.05em; }\n"
                + "    .mark { color: #fff; border-radius: 50%; width: 22px; height: 22px; display: inline-flex;\n"
                + "            align-items: center; justify-content: center; font-size: 0.8em; }\n"
                + "    .soft { font-weight: normal; font-size: 0.75em; color: #666; border: 1px solid #ccc;\n"
                + "            border-radius: 10px; padding: 1px 8px; margin-left: 6px; }\n"
                + "    .meta { color: #444; font-size: 0.88em; margin-top: 8px; }\n"
                + "    .fail { color: #c62828; font-size: 0.88em; margin-top: 6px; }\n"
                + "    .detail { color: #555; font-size: 0.82em; margin-top: 4px; }\n"
                + "    .detail code { color: #000; }\n"
                + "    .error { background: #fdecea; color: #b71c1c; border-radius: 4px; padding: 8px 10px;\n"
                + "             margin-top: 8px; font-family: monospace; font-size: 0.82em; }\n"
                + "    table.instance { border-collapse: collapse; width: 100%; }\n"
                + "    table.instance th, table.instance td { padding: 6px 12px; text-align: left;\n"
                + "             border-bottom: 1px solid #eee; vertical-align: top; }\n"
                + "    table.instance th { width: 180px; color: #555; }\n"
                + "    .note { color: #666; font-size: 0.85em; margin-top: 4px; }\n"
                + "  </style>\n</head>\n<body>\n"
                + "  <h1>OBP Hola &mdash; System Status</h1>\n"
                + "  <div class=\"sub\">Periodic server-side health checks &mdash; each card shows when its check last ran. "
                + "This page reloads every 15s.</div>\n"
                + "  <div class=\"summary\">\n"
                + "    <div class=\"overall\" style=\"color: " + overallColour + ";\">" + summary.getOverallStatus() + "</div>\n"
                + "    <div class=\"counts\">\n"
                + "      <div class=\"count\"><b>" + summary.getTotal() + "</b><span>Total Services</span></div>\n"
                + "      <div class=\"count\"><b style=\"color:#2e7d32;\">" + summary.getHealthy() + "</b><span>Healthy</span></div>\n"
                + "      <div class=\"count\"><b style=\"color:#c62828;\">" + summary.getUnhealthy() + "</b><span>Unhealthy</span></div>\n"
                + "      <div class=\"count\"><b style=\"color:#757575;\">" + (summary.getUnknown()) + "</b><span>Unknown / Stale</span></div>\n"
                + "    </div>\n"
                + "    <div class=\"count\"><b>" + summary.getHealthPercentage() + "%</b><span>Healthy</span></div>\n"
                + "  </div>\n"
                + "  <h2>Instance</h2>\n  <table class=\"instance\">\n"
                + "    <tr><th>app</th><td>obp-hola</td></tr>\n"
                + "    <tr><th>version</th><td>" + htmlEscape(appVersion()) + "</td></tr>\n"
                + "    <tr><th>git_commit</th><td><code>" + htmlEscape(gitCommit()) + "</code>"
                + (gitDirty()
                    ? " <b style=\"color:#ef6c00;\">dirty</b>"
                      + "<div class=\"note\">built from a working tree with uncommitted changes &mdash; "
                      + "this names the last commit, not necessarily what is running</div>"
                    : "")
                + "</td></tr>\n"
                + "    <tr><th>git_branch</th><td>" + htmlEscape(gitBranch()) + "</td></tr>\n"
                + "    <tr><th>build_time</th><td>" + htmlEscape(gitBuildTime()) + "</td></tr>\n"
                + "    <tr><th>uptime_seconds</th><td>" + uptimeSeconds() + "</td></tr>\n"
                + "  </table>\n"
                + "  <h2>Service Details</h2>\n"
                + cards
                + "  <p class=\"sub\">generated " + htmlEscape(now.truncatedTo(ChronoUnit.SECONDS).toString()) + "</p>\n"
                + "</body>\n</html>";
    }
}

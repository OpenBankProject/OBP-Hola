package com.openbankproject;

// RequestResponseLogger.java
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RequestResponseLogger {

    @Autowired
    private RedisService redisService;

    public StringBuilder saveRequestInfoToRedis(HttpRequest request, String body) {
        StringBuilder logEntry = buildRequestInfo(request, body);
        String key = "log-entry-for-session-id: " + SessionUtils.getSessionId();
        String value = logEntry.toString();
        redisService.saveLogToRedis(key, value);
        return logEntry;
    }

    private StringBuilder buildRequestInfo(HttpRequest request, String body) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("============= Request begin ").append(DateTimeUtils.printUtcDateTime()).append(" =============\n")
                .append("=== Session ID: ").append(SessionUtils.getSessionId()).append("\n")
                .append("=== Status Line : ").append(request.getRequestLine()).append("\n")
                .append("=== Headers : ").append(StringUtils.join(request.getAllHeaders(), "; ")).append("\n")
                .append("=== Request body: ").append(body).append("\n")
                .append("============= Request end ").append(DateTimeUtils.printUtcDateTime()).append(" =============\n");
        return logEntry;
    }

    public StringBuilder saveResponseInfoToRedis(HttpResponse response, String body) {
        StringBuilder logEntry = buildResponseInfo(response, body);
        String key = "log-entry-for-session-id: " + SessionUtils.getSessionId();
        String value = logEntry.toString();
        redisService.saveLogToRedis(key, value);
        return logEntry;
    }

    private StringBuilder buildResponseInfo(HttpResponse response, String body) {
        StringBuilder logEntry = new StringBuilder();
        logEntry.append("============= Response begin ").append(DateTimeUtils.printUtcDateTime()).append(" =============\n")
                .append("=== Session ID: ").append(SessionUtils.getSessionId()).append("\n")
                .append("=== Status Line : ").append(response.getStatusLine()).append("\n")
                .append("=== Headers : ").append(StringUtils.join(response.getAllHeaders(), "; ")).append("\n")
                .append("=== Response body: ").append(body).append("\n")
                .append("============= Response end ").append(DateTimeUtils.printUtcDateTime()).append(" =============\n");
        return logEntry;
    }
}

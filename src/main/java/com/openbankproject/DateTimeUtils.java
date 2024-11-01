package com.openbankproject;

// DateTimeUtils.java
import java.time.Instant;

public class DateTimeUtils {
    public static String printUtcDateTime() {
        Instant now = Instant.now();
        return "at UTC Time: " + now.toString();
    }
}

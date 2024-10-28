package com.openbankproject;

// SessionUtils.java
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class SessionUtils {
    public static String getSessionId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null && attributes.getRequest() != null && attributes.getRequest().getSession(false) != null) {
            return attributes.getRequest().getSession(false).getId();  // Get the existing session, don't create a new one
        }
        return "";
    }
}


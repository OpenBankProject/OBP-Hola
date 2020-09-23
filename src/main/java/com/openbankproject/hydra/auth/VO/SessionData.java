package com.openbankproject.hydra.auth.VO;

import javax.servlet.http.HttpSession;

/**
 * informations keet in session
 */
public class SessionData {
    private String[] selectConsents;
    private String bankId;
    private String[] allAccountIds;
    private String[] selectAccountIds;
    private String code;
    private String idToken;
    private String accessToken;
    private String refreshToken;
    private String state;
    private UserInfo userInfo;

    private SessionData() {}

    public static void setSelectConsents(HttpSession session, String[] selectConsents) {
        getOrCreateSessionData(session).selectConsents = selectConsents;
    }
    public static boolean hasSelectConsents(HttpSession session) {
        return getOrCreateSessionData(session).selectConsents != null;
    }

    public static void setBankId(HttpSession session, String bankId) {
        getOrCreateSessionData(session).bankId = bankId;
    }
    public static boolean hasBankId(HttpSession session) {
        return getOrCreateSessionData(session).bankId != null;
    }

    public static void setAllAccountIds(HttpSession session, String[] allAccountIds) {
        getOrCreateSessionData(session).allAccountIds = allAccountIds;
    }
    public static boolean hasAllAccountIds(HttpSession session) {
        return getOrCreateSessionData(session).allAccountIds != null;
    }

    public static void setSelectAccountIds(HttpSession session, String[] selectAccountIds) {
        getOrCreateSessionData(session).selectAccountIds = selectAccountIds;
    }
    public static boolean hasSelectAccountIds(HttpSession session) {
        return getOrCreateSessionData(session).selectAccountIds != null;
    }

    public static void setCode(HttpSession session, String code) {
        getOrCreateSessionData(session).code = code;
    }
    public static boolean hasCode(HttpSession session) {
        return getOrCreateSessionData(session).code != null;
    }

    public static void setIdToken(HttpSession session, String idToken) {
        getOrCreateSessionData(session).idToken = idToken;
    }
    public static boolean hasIdToken(HttpSession session) {
        return getOrCreateSessionData(session).idToken != null;
    }

    public static void setAccessToken(HttpSession session, String accessToken) {
        getOrCreateSessionData(session).accessToken = accessToken;
    }
    public static boolean hasAccessToken(HttpSession session) {
        return getOrCreateSessionData(session).accessToken != null;
    }

    public static void setRefreshToken(HttpSession session, String refreshToken) {
        getOrCreateSessionData(session).refreshToken = refreshToken;
    }
    public static boolean hasRefreshToken(HttpSession session) {
        return getOrCreateSessionData(session).refreshToken != null;
    }

    public static void setState(HttpSession session, String state) {
        getOrCreateSessionData(session).state = state;
    }
    public static boolean hasState(HttpSession session) {
        return getOrCreateSessionData(session).state != null;
    }

    public static void setUserInfo(HttpSession session, UserInfo userInfo) {
        getOrCreateSessionData(session).userInfo = userInfo;
    }
    public static boolean hasUserInfo(HttpSession session) {
        return getOrCreateSessionData(session).userInfo != null;
    }
//////////////////////////////////////////////////////////////////////////

    public static String[] getSelectConsents(HttpSession session) {
        return getOrCreateSessionData(session).selectConsents;
    }

    public static String getBankId(HttpSession session) {
        return getOrCreateSessionData(session).bankId;
    }

    public static String[] getAllAccountIds(HttpSession session) {
        return getOrCreateSessionData(session).allAccountIds;
    }

    public static String[] getSelectAccountIds(HttpSession session) {
        return getOrCreateSessionData(session).selectAccountIds;
    }

    public static String getCode(HttpSession session) {
        return getOrCreateSessionData(session).code;
    }

    public static String getIdToken(HttpSession session) {
        return getOrCreateSessionData(session).idToken;
    }

    public static String getAccessToken(HttpSession session) {
        return getOrCreateSessionData(session).accessToken;
    }

    public static String getRefreshToken(HttpSession session) {
        return getOrCreateSessionData(session).refreshToken;
    }

    public static String getState(HttpSession session) {
        return getOrCreateSessionData(session).state;
    }

    public static UserInfo getUserInfo(HttpSession session) {
        return getOrCreateSessionData(session).userInfo;
    }

    public static boolean isAuthenticated(HttpSession session) {
        SessionData sessionData = (SessionData) session.getAttribute("session_data");
        return sessionData != null;
    }

    private static SessionData getOrCreateSessionData(HttpSession session) {
        SessionData sessionData = (SessionData) session.getAttribute("session_data");
        if(sessionData == null) {
            sessionData = new SessionData();
            session.setAttribute("session_data", sessionData);
        }
        return sessionData;
    }
}

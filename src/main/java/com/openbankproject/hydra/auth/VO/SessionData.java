package com.openbankproject.hydra.auth.VO;

import javax.servlet.http.HttpSession;

/**
 * informations keet in session
 */
public class SessionData {
    private String[] selectAccountIds;
    private String code;
    private String idToken;
    private String accessToken;
    private String refreshToken;
    private String state;
    private String nonce;
    private String codeVerifier;
    private String recurringIndicator;
    private String frequencyPerDay;
    private String expirationTime;
    private String bankId;
    private String apiStandard;
    private String consentId;
    private UserInfo userInfo;

    public static String getConsentId(HttpSession session) {
        return getOrCreateSessionData(session).consentId;
    }

    public static void setConsentId(HttpSession session, String consentId) {
        getOrCreateSessionData(session).consentId = consentId;
    }

    public static String getApiStandard(HttpSession session) {
        return getOrCreateSessionData(session).apiStandard;
    }

    public static void setApiStandard(HttpSession session, String apiStandard) {
        getOrCreateSessionData(session).apiStandard = apiStandard;
    }

    public static String getBankId(HttpSession session) {
        return getOrCreateSessionData(session).bankId;
    }

    public static void setBankId(HttpSession session, String bankId) {
        getOrCreateSessionData(session).bankId = bankId;
    }

    public static String getRecurringIndicator(HttpSession session) {
        return getOrCreateSessionData(session).recurringIndicator;
    }

    public static void setRecurringIndicator(HttpSession session, String recurringIndicator) {
        getOrCreateSessionData(session).recurringIndicator = recurringIndicator;
    }

    public static String getExpirationTime(HttpSession session) {
        return getOrCreateSessionData(session).expirationTime;
    }

    public static void setExpirationTime(HttpSession session, String expirationTime) {
        getOrCreateSessionData(session).expirationTime = expirationTime;
    }

    public static String getFrequencyPerDay(HttpSession session) {
        return getOrCreateSessionData(session).frequencyPerDay;
    }

    public static void setFrequencyPerDay(HttpSession session, String frequencyPerDay) {
        getOrCreateSessionData(session).frequencyPerDay = frequencyPerDay;
    }

    private SessionData() {}

    public static void setSelectAccountIds(HttpSession session, String[] selectAccountIds) {
        getOrCreateSessionData(session).selectAccountIds = selectAccountIds;
    }
    public static boolean hasSelectAccountIds(HttpSession session) {
        return getOrCreateSessionData(session).selectAccountIds != null;
    }

    public static void setCode(HttpSession session, String code) {
        getOrCreateSessionData(session).code = code;
    }

    public static void setIdToken(HttpSession session, String idToken) {
        getOrCreateSessionData(session).idToken = idToken;
    }

    public static void setAccessToken(HttpSession session, String accessToken) {
        getOrCreateSessionData(session).accessToken = accessToken;
    }

    public static void setRefreshToken(HttpSession session, String refreshToken) {
        getOrCreateSessionData(session).refreshToken = refreshToken;
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
    public static void remoteUserInfo(HttpSession session) {
        setUserInfo(session, null);
    }
    public static void setNonce(HttpSession session, String nonce) {
        getOrCreateSessionData(session).nonce = nonce;
    }
//////////////////////////////////////////////////////////////////////////

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

    public static String getNonce(HttpSession session) {
        return getOrCreateSessionData(session).nonce;
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

    public static String getCodeVerifier(HttpSession session) {
        return getOrCreateSessionData(session).codeVerifier;
    }

    public static void setCodeVerifier(HttpSession session, String codeVerifier) {
        getOrCreateSessionData(session).codeVerifier = codeVerifier;
    }
}

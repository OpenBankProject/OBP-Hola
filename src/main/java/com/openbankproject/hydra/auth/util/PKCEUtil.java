package com.openbankproject.hydra.auth.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.Base64;

public interface PKCEUtil {

    static String generateCodeVerifier() { ;
        final byte[] randomBytes = RandomUtils.nextBytes(64);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    static String generateCodeChallenge(String codeVerifier) {
        byte[] digest =  DigestUtils.sha256(codeVerifier);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
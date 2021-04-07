package com.openbankproject;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class JwsUtil {

    public static String createJwsSignature(RSAKey privateKey, String x5c, String verb, String url, Map<String, String> requestHeaders, String httpBody) {

        String sigD = "{\n" +
                "\"pars\": [\n" +
                "\"(request-target)\",\n" +
                "\"host\",\n" +
                "\"content-type\",\n" +
                "\"psu-ip-address\",\n" +
                "\"psu-geo-location\",\n" +
                "\"digest\"\n" +
                "],\n" +
                "\"mId\": \"http://uri.etsi.org/19182/HttpHeaders\"\n" +
                "}\n";

        String digest = computeDigest(httpBody);
        Payload detachedPayload = new Payload(
                "(request-target): " + verb + " " + url + "\n" +
                        "host: " + requestHeaders.get("host") + "\n" +
                        "content-type: " + requestHeaders.get("content-type") + "\n" +
                        "psu-ip-address: " + requestHeaders.get("psu-ip-address") + "\n" +
                        "psu-geo-location: " + requestHeaders.get("psu-geo-location") + "\n" +
                        "digest: SHA-256=" + digest + "\n"
        );

        // We create the time in next format: '2011-12-03T10:15:30Z' 
        String sigT = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        Set<String> criticalParams = getCriticalHeaders();
        //JWK jwk  = null;
        RSASSASigner signer  = null;
        JWSHeader jwsProtectedHeader = null;
        JWSObject jwsObject = null;
        try {
            signer = new RSASSASigner(privateKey);

            criticalParams.addAll(getDeferredCriticalHeaders());
            List<com.nimbusds.jose.util.Base64> x5cList = new ArrayList<>();
            x5cList.add(new com.nimbusds.jose.util.Base64(x5c));
            jwsProtectedHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .base64URLEncodePayload(false)
                    //.x509CertSHA256Thumbprint(privateKey.computeThumbprint())
                    .x509CertChain(x5cList)
                    .criticalParams(criticalParams)
                    .customParam("sigT", sigT)
                    .customParam("sigD", JSONObjectUtils.parse(sigD))
                    .build();

            // Compute the RSA signature
            jwsObject = new JWSObject(jwsProtectedHeader, detachedPayload);
            jwsObject.sign(signer);
        } catch (JOSEException | ParseException e) {
            e.printStackTrace();
        }
        
        boolean isDetached = true;
        String jws = jwsObject.serialize(isDetached);
        return jws;
    }

    // Base64 encoded sha256
    public static String computeDigest(String input) {
        String encodedString = Base64.getEncoder().encodeToString(DigestUtils.sha256(input));
        return encodedString; 
    }
    public static String createDigestHeaderValue(String input) { return "SHA-256=" + computeDigest(input); }
    public static Boolean verifyDigestHeader(String headerValue, String httpBody) {
        if (headerValue.compareTo("SHA-256=" + computeDigest(httpBody)) == 0) {
            return true;
        }
        else {
            return false;
        }
    }

    public static Set<String> getDeferredCriticalHeaders() {
        Set<String> deferredCriticalHeaders = new HashSet<>();
        deferredCriticalHeaders.add("sigT");
        deferredCriticalHeaders.add("sigD");
        return deferredCriticalHeaders;
    }
    public static Set<String> getCriticalHeaders() {
        Set<String> criticalHeaders = new HashSet<>();
        criticalHeaders.add("b64");
        return criticalHeaders;
    }
    
}

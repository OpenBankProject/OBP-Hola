package com.openbankproject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jose.util.X509CertUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class JwsUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwsUtil.class);

    public static boolean verifyJwsSignature(String sigT, 
                                             String httpBody,
                                             String xJwsSignature,
                                             String digest,
                                             String pem,
                                             String rebuiltDetachedPayload) {
        
        // Check Signing Time
        ZonedDateTime signingTime = ZonedDateTime.parse(sigT, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        ZonedDateTime verifyingTime = ZonedDateTime.now(ZoneOffset.UTC);
        boolean criteriaOneFailed = signingTime.isAfter(verifyingTime.plusSeconds(2));
        boolean criteriaTwoFailed = signingTime.plusSeconds(60).isBefore(verifyingTime);
        boolean isSigningTimeOk = !criteriaOneFailed && !criteriaTwoFailed;
                
        // Check HTTP Body
        String computedDigest = "SHA-256=" + computeDigest(httpBody);
        boolean isDigestOk = computedDigest.equals(digest);
                
        boolean isVerifiedJws = false;
        try {
            // Parse JWS with detached payload
            JWSObject parsedJWSObject = JWSObject.parse(xJwsSignature, new Payload(rebuiltDetachedPayload));
            // Parse X.509 certificate
            X509Certificate cert = X509CertUtils.parse(pem);
            // Retrieve public key as RSA JWK
            RSAKey jwkPublic = null;
            jwkPublic = RSAKey.parse(cert);
            // Verify the RSA
            RSASSAVerifier verifier = new RSASSAVerifier(jwkPublic.toRSAKey().toRSAPublicKey(), getDeferredCriticalHeaders());
            isVerifiedJws = parsedJWSObject.verify(verifier);
        } catch (JOSEException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return isVerifiedJws && isDigestOk && isSigningTimeOk;
    }

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
        logger.debug("<--------------------- Detached Payload --------------------->\n");
        logger.debug("\n" + detachedPayload.toString());
        logger.debug("<--------------------- Detached Payload --------------------->\n");

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
            logger.debug("JWS Protected Header: \n" + jwsProtectedHeader.toString());

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

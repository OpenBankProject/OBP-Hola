package com.openbankproject;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.apache.commons.codec.digest.DigestUtils;

import java.text.ParseException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JwsUtil {

    public static void main(String[] args) {
        String verb = "post";
        String url = "/berlin-group/v1.3/payments/sepa-credit-transfers";
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("host", "api.testbank.com");
        requestHeaders.put("content-type", "application/json");
        requestHeaders.put("psu-ip-address", "192.168.8.78");
        requestHeaders.put("psu-geo-location", "GEO:52.506931,13.144558");
        String httpBody =
                "{\n" +
                        "\"instructedAmount\": {\"currency\": \"EUR\", \"amount\": \"123.50\"},\n" +
                        "\"debtorAccount\": {\"iban\": \"DE40100100103307118608\"},\n" +
                        "\"creditorName\": \"Merchant123\",\n" +
                        "\"creditorAccount\": {\"iban\": \"DE02100100109307118603\"},\n" +
                        "\"remittanceInformationUnstructured\": \"Ref Number Merchant\"\n" +
                        "}\n";
        System.out.println(createJwsSignature(verb, url, requestHeaders, httpBody));
    }

    public static String createJwsSignature(String verb, String url, Map<String, String> requestHeaders, String httpBody) {
        String pemEncodedRSAPrivateKey =
                "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEpAIBAAKCAQEAyaWz5PDC+WAjzKVni66t0aB6UcMeaLScdospNgT32GmE2jfT\n" +
                "zUGes0OWV4C6JN1XQXeeTwgzqX0n3IR+fQCs+3o1G3Cu0c3f8as7TQZv9Gdy41re\n" +
                "HfYNtmz8pxGO5e/tVyyIsU2J+IZRjn+6glL+00vegf/SDvGaZuJrs2LRPnmUgypX\n" +
                "5KcUTRM+XjR3tFpFaFm3k1ns9qn+6lunDszgLwAQ4OFSuoq0w457TOrStdvQxRjw\n" +
                "Aa/2XxgDX8qU/FjTfm1Shjmh4vO0nGYDyvJnLKo5/Q/txTIt4gOCxg/I67pdY8AS\n" +
                "FxeBaq70sebjMSBt2a++ig5XIQraW6VmJSCl7QIDAQABAoIBAAyQdhaQR93I90IT\n" +
                "llGWRz9WC/kXOshUZKFgR2eVxKmn3X7JVrml2pEZ5365Rx/v6LVsEiGjhbCMW1T6\n" +
                "rnT0e1LKCRAWI9ZvyQHiZPYGLiig34A6E7fzMmSJAu8YAXrjSbsSS8wcZDnniKJj\n" +
                "5Aelyzn4MruP6JNEy5WYixRo1lfZmC52M4WBwyzbRyzmPlnYKJTJ6l9z3CXgvUmw\n" +
                "gvPSahgIyxALgqhNRh9ngMBJQG7Hkag2MsLUz+2fncHoKUHYEmdAjb2cwXYSClzg\n" +
                "VLYeBe2m/vuWafD0v53DAkyIJv4knHuaUUC5adJ/cQzoAWNAglWkWxtskNlPfZHx\n" +
                "4VwqR7MCgYEA5AWjOfHC5erY+205ukYkcRyrPBFOLSSr4RUE/NDsd2w+cryYeQZg\n" +
                "SvsbFvOZH2IOnMCyfcUNe3ExNwaRFP7hCI9H449F0AwUFcQs604K++a2y/7TvlnZ\n" +
                "q+/Ahu7SueVUHdi+9eL4jwyuxVUJg8UhHYHD9oqW4Af/bHvJYy0IYucCgYEA4mOc\n" +
                "vx1456kSW0EiqJyJ3yUAUNrVY75AfwprWCBjgIYkEYEAHaQ/EyHnRGO5ySRYGYs6\n" +
                "GAWuYBHyPzKUnIuxpCutaU/hWjw2w/6T9hLYzyEn5NG7EhaDMiO/pEIpX0BZ29mx\n" +
                "yIoXc8SEDZzuiB+tVuqjW24rLyLNn3+ppr9RqgsCgYEAusLbVGRuC771BcoKlEVL\n" +
                "J9Ihdkt+Sn9UwEBlG2VLqOzhoTxTbh0I1aEiKQRQkGHSMhWqnFS/nDGz66vXPOke\n" +
                "C9K/QOVieurJsKJDYF9Fo9juM9t+NtSE8symVl5Z/qSU5vVWQzMp/pCWvU3PQzw8\n" +
                "yVw100LkHI6waHxjEHYb/lUCgYAbGXt48Sk46ec1nz1r25kxafd4tklW8D4+NtwU\n" +
                "p4Phra0Bn2SJJ9EZFDTf3eQubLhTDnR8zalK/Lr3z7E0cBBqq4PNmG9MYurXWVES\n" +
                "4ryrRrfEz0pKZwF7bgYRvo2/Ri+7fnqmm8kk5YA9NOzkxI32Wo4FctGeidb9YcXI\n" +
                "HRzEcwKBgQDKqCBgSnf414PP+KzIhQYUXCbXetwcNGwnPh3VkB0DXiROnpg/x5VJ\n" +
                "VqB2rXZWLnCgp36H+fF+hvFzcIGiSloSREAhPhDMdC6vCP39CR2b7Nik3txVuqK3\n" +
                "oF4inGgJYgKfsLaxEhpo64l1IPXCB2zmE2eNeZt+0YE82T+ad+XIUw==\n" +
                "-----END RSA PRIVATE KEY-----";

        String pemEncodedCertificate =
                "-----BEGIN CERTIFICATE-----\n" +
                "MIICsjCCAZqgAwIBAgIGAXiEVYd0MA0GCSqGSIb3DQEBCwUAMBoxGDAWBgNVBAMM\n" +
                "D2FwcC5leGFtcGxlLmNvbTAeFw0yMTAzMzAxODExNDFaFw0yMzAzMzAxODExNDFa\n" +
                "MBoxGDAWBgNVBAMMD2FwcC5leGFtcGxlLmNvbTCCASIwDQYJKoZIhvcNAQEBBQAD\n" +
                "ggEPADCCAQoCggEBAMmls+TwwvlgI8ylZ4uurdGgelHDHmi0nHaLKTYE99hphNo3\n" +
                "081BnrNDlleAuiTdV0F3nk8IM6l9J9yEfn0ArPt6NRtwrtHN3/GrO00Gb/RncuNa\n" +
                "3h32DbZs/KcRjuXv7VcsiLFNifiGUY5/uoJS/tNL3oH/0g7xmmbia7Ni0T55lIMq\n" +
                "V+SnFE0TPl40d7RaRWhZt5NZ7Pap/upbpw7M4C8AEODhUrqKtMOOe0zq0rXb0MUY\n" +
                "8AGv9l8YA1/KlPxY035tUoY5oeLztJxmA8ryZyyqOf0P7cUyLeIDgsYPyOu6XWPA\n" +
                "EhcXgWqu9LHm4zEgbdmvvooOVyEK2lulZiUgpe0CAwEAATANBgkqhkiG9w0BAQsF\n" +
                "AAOCAQEAUZ5BqGFl+ce2skZD6Wf3PnCsdos9HnSFg/WogcDbeTVLS3+bn5Z+VbOK\n" +
                "m4yroOB9VCfhZ6msrBKKcELmj85jRGw4bnip9AjOrYgowePr0f0kq/BMIkZkSB9J\n" +
                "98PusxUXgfVdeygbfLyBhQkkGKYaIEIsWSyFxJ/grIoZSwdIt2wkD2VfGakdq9UI\n" +
                "1IY8BK9NiILGL3HaQdoHFavuXq6/M6/5hrIRtpXncZTO93i4/YhLuaPWlXjGqijL\n" +
                "dfo4IrZChEBioZ/sGmatrqoQUdeuG1loMWFPRN6/6AdXv33QAL7uiKTWpFOJkMys\n" +
                "Waq0JntfTgPHs/yvCjzdKrOKS1uUgQ==\n" +
                "-----END CERTIFICATE-----";

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
        JWK jwk  = null;
        RSASSASigner signer  = null;
        JWSHeader jwsProtectedHeader = null;
        JWSObject jwsObject = null;
        try {
            jwk = JWK.parseFromPEMEncodedObjects(pemEncodedRSAPrivateKey);
            RSAKey rsaJWK  = jwk.toRSAKey();
            // Create RSA-signer with the private key
            signer = new RSASSASigner(rsaJWK);
            
            criticalParams.addAll(getDeferredCriticalHeaders());
            jwsProtectedHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .base64URLEncodePayload(false)
                    .x509CertSHA256Thumbprint(jwk.computeThumbprint())
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

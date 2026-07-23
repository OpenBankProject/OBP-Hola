package com.openbankproject.hydra.auth;

import com.openbankproject.RequestResponseLogger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Hola's side of the mutual TLS link to OBP-API.
 *
 * Builds the real {@link RestTemplateConfig} against the real committed stores — the same objects
 * the {@code mtls} profile wires — rather than a Spring context, because the application context
 * eagerly fetches an OIDC discovery document and so cannot start without an identity provider.
 * Nothing here needs Keycloak; the transport is what is under test.
 *
 * The call to OBP-API is skipped rather than failed when nothing is listening, so this stays
 * useful in a checkout with no server running. The parts that need no server always run.
 */
class MtlsToObpApiTest {

    private static final String OBP_API = "https://localhost:8080";
    private static final char[] PASSWORD = "123456".toCharArray();

    private RestTemplateConfig mtlsConfig() {
        RestTemplateConfig config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "keyStoreResource", new ClassPathResource("cert/hola-client.p12"));
        ReflectionTestUtils.setField(config, "keyStorePassword", PASSWORD);
        ReflectionTestUtils.setField(config, "keyStoreAlias", "hola-client");
        ReflectionTestUtils.setField(config, "trustStoreResource", new ClassPathResource("cert/hola-truststore.p12"));
        ReflectionTestUtils.setField(config, "trustStorePassword", PASSWORD);
        // Stubbed: the interceptors log every request and response to Redis, which is beside the
        // point here and would make the test need a Redis to talk to. Both methods return a
        // StringBuilder the interceptor dereferences, so a bare mock returning null is not enough.
        RequestResponseLogger logger = Mockito.mock(RequestResponseLogger.class);
        Mockito.when(logger.saveRequestInfoToRedis(Mockito.any(), Mockito.any()))
                .thenReturn(new StringBuilder());
        Mockito.when(logger.saveResponseInfoToRedis(Mockito.any(), Mockito.any()))
                .thenReturn(new StringBuilder());
        ReflectionTestUtils.setField(config, "requestResponseLogger", logger);
        // @Value("${force_jws:}") resolves to "" under Spring; left null here it NPEs in the
        // request interceptor before the request is sent.
        ReflectionTestUtils.setField(config, "forceJws", "");
        return config;
    }

    private KeyStore load(String resource) throws IOException, GeneralSecurityException {
        // PKCS12 explicitly here: the point of the assertions below is that the production code
        // picks the type itself, so the test must not borrow that decision from it.
        KeyStore store = KeyStore.getInstance("PKCS12");
        try (java.io.InputStream in = new ClassPathResource(resource).getInputStream()) {
            store.load(in, PASSWORD);
        }
        return store;
    }

    @Test
    void holaPresentsAnIdentityNamedForTheTppRatherThanForAHost() throws Exception {
        KeyStore store = load("cert/hola-client.p12");
        X509Certificate certificate = (X509Certificate) store.getCertificate("hola-client");
        assertNotNull(certificate, "hola-client.p12 should hold an entry aliased hola-client");

        String subject = certificate.getSubjectX500Principal().getName();
        assertTrue(subject.contains("CN=obp-hola"), "expected CN=obp-hola, got " + subject);
        // The predecessor of this certificate was CN=localhost, O=TESOBE GmbH: a host name for a
        // party, and the bank's organisation for the TPP's.
        assertTrue(subject.contains("O=Hola Ltd"), "expected O=Hola Ltd, got " + subject);
    }

    @Test
    void holaCertificateIsIssuedByTheCaThatObpApiTrusts() throws Exception {
        X509Certificate client =
                (X509Certificate) load("cert/hola-client.p12").getCertificate("hola-client");
        X509Certificate ca =
                (X509Certificate) load("cert/hola-truststore.p12").getCertificate("obp-dev-ca");

        assertNotNull(ca, "hola-truststore.p12 should hold the OBP development CA");
        assertEquals(ca.getSubjectX500Principal(), client.getIssuerX500Principal(),
                "Hola's certificate must be signed by the CA OBP-API's truststore trusts");
        // Proves the signature, not merely that the names line up.
        client.verify(ca.getPublicKey());
    }

    @Test
    void trustStoreHoldsTheCaAndNothingElse() throws Exception {
        // Pinning OBP-API's server leaf instead would break the next time it regenerates its set.
        assertEquals(1, java.util.Collections.list(load("cert/hola-truststore.p12").aliases()).size());
    }

    @Test
    void restTemplateReachesObpApiOverMutualTls() throws Exception {
        assumeTrue(isListening(), "OBP-API is not running on localhost:8080 — skipping the live call");

        RestTemplate restTemplate = mtlsConfig().restTemplate();
        String body = restTemplate.getForObject(OBP_API + "/obp/v5.1.0/root", String.class);

        assertNotNull(body);
        assertTrue(body.contains("\"version\""), "expected the root JSON, got: " + body);
    }

    private boolean isListening() {
        try (Socket socket = new Socket("localhost", 8080)) {
            return socket.isConnected();
        } catch (IOException e) {
            return false;
        }
    }
}

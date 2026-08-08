package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.SessionData;
import com.openbankproject.hydra.auth.VO.WellKnown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * How the Berlin Group consent-information call has to authenticate.
 *
 * Not the way the data calls do. The data endpoints take a Consent-ID header because the consent is
 * the credential; this one carries the consent id in its *path*, and OBP refuses a Consent-ID header
 * on the whole /consents/... family for exactly that reason -- OBP-20256, "Consent-Id must not be
 * used for this API Endpoint". The call was sending Consent-ID anyway, so every attempt was refused
 * and the failure only ever reached a log line: the status, frequency, recurring indicator and
 * validity shown on the accounts page were silently blank on every Berlin Group run.
 *
 * Driven standalone for the same reason as the sibling tests: ObpOidcConfig fetches the OIDC
 * well-known document from @PostConstruct, so a real context needs a running OBP-OIDC.
 */
class BerlinGroupConsentInfoTest {

    private static final String BASE = "http://obp.test";
    private static final String TOKEN_ENDPOINT = "http://oidc.test/token";
    private static final String CONSENT_ID = "consent-under-test";

    private MockMvc mockMvc;
    private MockRestServiceServer obp;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        obp = MockRestServiceServer.bindTo(restTemplate).build();

        WellKnown wellKnown = new WellKnown();
        wellKnown.tokenEndpoint = TOKEN_ENDPOINT;

        IndexController controller = new IndexController();
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(controller, "openIDConfiguration", wellKnown);
        ReflectionTestUtils.setField(controller, "clientId", "test-client-id");
        ReflectionTestUtils.setField(controller, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(controller, "getConsentInformationBerlinGroup",
                BASE + "/berlin-group/v1.3/consents/CONSENT_ID");

        // main() logs to Redis before anything else; a no-op stand-in keeps the test off the network.
        ReflectionTestUtils.setField(controller, "redisService", new com.openbankproject.RedisService() {
            @Override
            public void readLogFromRedis(javax.servlet.http.HttpSession s, org.springframework.ui.Model m) {
            }
        });

        // The handler returns the view name "main", which is also the request path -- without a
        // resolver that maps it elsewhere, standalone setup treats that as a circular dispatch.
        // The assertions here are on the outbound calls, which happen before any view is resolved.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new org.springframework.web.servlet.view.InternalResourceViewResolver("/WEB-INF/views/", ".jsp"))
                .build();
        session = new MockHttpSession();
        SessionData.setConsentId(session, CONSENT_ID);
        SessionData.setApiStandard(session, "BerlinGroup");
    }

    @Test
    @DisplayName("consent information is fetched as the TPP, without the forbidden Consent-ID header")
    void consentInfoAuthenticatesAsTheTpp() throws Exception {
        obp.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token-under-test\"}",
                        MediaType.APPLICATION_JSON));

        obp.expect(requestTo(BASE + "/berlin-group/v1.3/consents/" + CONSENT_ID))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-under-test"))
                // The header whose presence OBP refuses this endpoint over.
                .andExpect(headerDoesNotExist("Consent-ID"))
                .andRespond(withSuccess(
                        "{\"consentStatus\":\"valid\",\"frequencyPerDay\":4,"
                        + "\"recurringIndicator\":true,\"validUntil\":\"2026-12-31\"}",
                        MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/main").session(session));

        obp.verify();
    }

    @Test
    @DisplayName("a field the API reports as null is left blank, not shown as the word \"null\"")
    void nullFieldsAreNotRenderedAsTheWordNull() throws Exception {
        obp.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withSuccess("{\"access_token\":\"token-under-test\"}",
                        MediaType.APPLICATION_JSON));

        // OBP returns validUntil as an explicit null when the consent has none.
        obp.expect(requestTo(BASE + "/berlin-group/v1.3/consents/" + CONSENT_ID))
                .andRespond(withSuccess(
                        "{\"consentStatus\":\"valid\",\"frequencyPerDay\":4,"
                        + "\"recurringIndicator\":true,\"validUntil\":null}",
                        MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/main").session(session));

        // Unset, so the template renders an empty span. String.valueOf would have made it "null" --
        // which prints identically to a real null in a bare assertion message, hence the type here.
        Object validUntil = session.getAttribute("validUntil");
        assertNull(validUntil, () -> "validUntil should be unset, but was the "
                + validUntil.getClass().getSimpleName() + " \"" + validUntil + "\"");
        // The neighbours still arrive, so this is not just "nothing was set at all".
        assertEquals("valid", session.getAttribute("consentStatus"));
        assertEquals("4", session.getAttribute("frequencyPerDay"));
    }

    @Test
    @DisplayName("a failed fetch clears the previous consent's values instead of leaving them on screen")
    void aFailedFetchDoesNotLeaveStaleValues() throws Exception {
        // What a previous, successful consent in this same session left behind.
        session.setAttribute("consentStatus", "valid");
        session.setAttribute("validUntil", "2026-12-31");
        session.setAttribute("frequencyPerDay", "4");
        session.setAttribute("recurringIndicator", "true");

        obp.expect(requestTo(TOKEN_ENDPOINT))
                .andRespond(withServerError());

        mockMvc.perform(get("/main").session(session));

        // Stale values presented as this consent's live status would be worse than showing nothing.
        assertNull(session.getAttribute("consentStatus"));
        assertNull(session.getAttribute("validUntil"));
        assertNull(session.getAttribute("frequencyPerDay"));
        assertNull(session.getAttribute("recurringIndicator"));
    }
}

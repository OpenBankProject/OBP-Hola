package com.openbankproject.hydra.auth.controller;

import com.openbankproject.hydra.auth.VO.SessionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What the Berlin Group calls must put on the wire, and what must come back when OBP refuses.
 *
 * Both halves were broken at once and each hid the other: the calls omitted Consumer-Key, so OBP
 * answered OBP-35001 "Consent not found"; and the account list turned that into a 200 carrying a
 * hand-written hint blaming an unfinished SCA, so the one visible explanation pointed away from
 * the real cause. The other four calls said nothing at all -- three had no catch, and payment
 * returned prose under a 200. This pins all of it.
 *
 * The controller is driven standalone rather than through @SpringBootTest: ObpOidcConfig fetches
 * the OIDC well-known document from @PostConstruct, so loading the real context needs a running
 * OBP-OIDC. That is why the one pre-existing test in this project is @Disabled. Here the outbound
 * RestTemplate is intercepted by MockRestServiceServer, so nothing leaves the JVM.
 *
 * The URLs under test are read from the production @Value defaults rather than restated here, so
 * `bookingStatus=both` is asserted against the shipped declaration -- restating it in the test
 * would only assert that the test agrees with itself.
 */
class BerlinGroupConsentCallsTest {

    private static final String BASE = "http://obp.test";
    private static final String CONSUMER_KEY = "test-consumer-key";
    private static final String CONSENT_ID = "consent-under-test";
    private static final String ACCOUNT_ID = "account-under-test";

    /** A refusal as OBP-API actually shapes it -- the code is what has to reach the browser. */
    private static final String OBP_REFUSAL =
            "{\"tppMessages\":[{\"category\":\"ERROR\",\"code\":\"403\","
            + "\"path\":\"/berlin-group/v1.3/accounts\","
            + "\"text\":\"OBP-20017: Current user does not have access to the view.\"}]}";

    private MockMvc mockMvc;
    private MockRestServiceServer obp;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        obp = MockRestServiceServer.bindTo(restTemplate).build();

        OtherController controller = new OtherController();
        ReflectionTestUtils.setField(controller, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(controller, "consumerKey", CONSUMER_KEY);
        for (String field : new String[]{
                "getBerlinGroupAccountsUrl", "getBerlinGroupAccountUrl", "getBerlinGroupBalanceUrl",
                "getBerlinGroupTransactionsUrl", "initiatePaymentBerlinGroupUrl"}) {
            ReflectionTestUtils.setField(controller, field, declaredUrl(field));
        }

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        session = new MockHttpSession();
        SessionData.setConsentId(session, CONSENT_ID);
    }

    /** The field's shipped @Value expression, with only the configurable base substituted. */
    private static String declaredUrl(String fieldName) {
        Field field = ReflectionUtils.findField(OtherController.class, fieldName);
        assertThat(field).as("field %s", fieldName).isNotNull();
        String declared = field.getAnnotation(Value.class).value();
        return declared.replace("${obp.base_url}", BASE);
    }

    private String url(String fieldName) {
        return declaredUrl(fieldName).replace("ACCOUNT_ID", ACCOUNT_ID);
    }

    // ── What goes out ───────────────────────────────────────────────────────

    @Test
    @DisplayName("every Berlin Group read carries both the consent and the consumer identifying it")
    void readsCarryConsentIdAndConsumerKey() throws Exception {
        // Consent-ID alone is not enough: OBP-API matches the requesting consumer against the one
        // the consent was lodged with, and with nothing to match on it refuses with OBP-35001,
        // which reads exactly like a consent that does not exist.
        String[][] calls = {
                {"/account_bg", "getBerlinGroupAccountsUrl"},
                {"/account_bg/" + ACCOUNT_ID, "getBerlinGroupAccountUrl"},
                {"/balances_bg/account_id/" + ACCOUNT_ID, "getBerlinGroupBalanceUrl"},
                {"/transactions_bg/account_id/" + ACCOUNT_ID, "getBerlinGroupTransactionsUrl"},
        };

        for (String[] call : calls) {
            obp.reset();
            obp.expect(requestTo(url(call[1])))
                    .andExpect(method(HttpMethod.GET))
                    .andExpect(header("Consent-ID", CONSENT_ID))
                    .andExpect(header("Consumer-Key", CONSUMER_KEY))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

            mockMvc.perform(get(call[0]).session(session)).andExpect(status().isOk());
            obp.verify();
        }
    }

    @Test
    @DisplayName("payment initiation carries them too")
    void paymentCarriesConsentIdAndConsumerKey() throws Exception {
        obp.expect(requestTo(url("initiatePaymentBerlinGroupUrl")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Consent-ID", CONSENT_ID))
                .andExpect(header("Consumer-Key", CONSUMER_KEY))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/initiate_payment_bg/DE02/Creditor/DE01/1/EUR").session(session))
                .andExpect(status().isOk());
        obp.verify();
    }

    @Test
    @DisplayName("the transactions call asks for a bookingStatus, which the endpoint requires")
    void transactionsAskForBookingStatus() throws Exception {
        // Mandatory in the Berlin Group specification and enforced by OBP-API with no default:
        // anything but booked, pending or both is refused with OBP-10034. Asserted against the
        // shipped @Value, so dropping it from the declaration fails here.
        assertThat(declaredUrl("getBerlinGroupTransactionsUrl"))
                .as("shipped transactions URL")
                .contains("bookingStatus=both");

        obp.expect(requestTo(url("getBerlinGroupTransactionsUrl")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/transactions_bg/account_id/" + ACCOUNT_ID).session(session))
                .andExpect(status().isOk());
        obp.verify();
    }

    // ── What comes back when OBP refuses ────────────────────────────────────

    @Test
    @DisplayName("a refusal reaches the browser with its status and OBP's own body")
    void refusalIsPassedThroughVerbatim() throws Exception {
        // Not 200-with-an-error-inside, and not a guess at the cause: the page has to be able to
        // show which of OBP-35001, OBP-35005 or OBP-20017 it actually was.
        String[][] calls = {
                {"/account_bg", "getBerlinGroupAccountsUrl"},
                {"/account_bg/" + ACCOUNT_ID, "getBerlinGroupAccountUrl"},
                {"/balances_bg/account_id/" + ACCOUNT_ID, "getBerlinGroupBalanceUrl"},
                {"/transactions_bg/account_id/" + ACCOUNT_ID, "getBerlinGroupTransactionsUrl"},
        };

        for (String[] call : calls) {
            obp.reset();
            obp.expect(requestTo(url(call[1])))
                    .andRespond(withStatus(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(OBP_REFUSAL));

            mockMvc.perform(get(call[0]).session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(content().json(OBP_REFUSAL));
            obp.verify();
        }
    }

    @Test
    @DisplayName("a refused payment is passed through the same way, not turned into prose")
    void refusedPaymentIsPassedThroughVerbatim() throws Exception {
        // This one used to answer 200 with a sentence and the body glued on -- the status was lost
        // and the browser was handed something that is not JSON to parse.
        obp.expect(requestTo(url("initiatePaymentBerlinGroupUrl")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(OBP_REFUSAL));

        mockMvc.perform(get("/initiate_payment_bg/DE02/Creditor/DE01/1/EUR").session(session))
                .andExpect(status().isForbidden())
                .andExpect(content().json(OBP_REFUSAL));
        obp.verify();
    }

    @Test
    @DisplayName("the account list no longer answers 200 while carrying a refusal")
    void accountListDoesNotDressARefusalUpAsSuccess() throws Exception {
        // The regression this replaces: a 200 body of {code, message, hint} where hint asserted the
        // consent "was never confirmed via SCA and is still in status received" -- stated as fact,
        // and shown even for a consent that was valid.
        obp.expect(requestTo(url("getBerlinGroupAccountsUrl")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(OBP_REFUSAL));

        String body = mockMvc.perform(get("/account_bg").session(session))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("hint").doesNotContain("never confirmed");
        obp.verify();
    }
}

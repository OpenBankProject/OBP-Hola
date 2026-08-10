package com.openbankproject.hydra.auth.controller;

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

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * What a VRP consent-request must carry when the PSU submits the form.
 *
 * The counterparty name is the PSU-facing answer to "who may be paid under this mandate". It was
 * bound from the form and then dropped on the floor -- the ToAccount was built with a literal empty
 * string -- so the approval screen had no payee to show, and the counterparty OBP creates from the
 * request was left unnamed. Nothing failed loudly; the mandate was simply anonymous.
 *
 * Driven standalone rather than through @SpringBootTest for the same reason as
 * BerlinGroupConsentCallsTest: ObpOidcConfig fetches the OIDC well-known document from
 * @PostConstruct, so a real context needs a running OBP-OIDC. Here both outbound calls -- the
 * client-credentials token and the consent-request POST -- are intercepted, so nothing leaves the
 * JVM.
 */
class VrpConsentRequestTest {

    private static final String TOKEN_ENDPOINT = "http://oidc.test/token";
    private static final String VRP_URL = "http://obp.test/obp/v5.1.0/consumer/vrp-consent-requests";
    private static final String COUNTERPARTY_NAME = "Landlord Standing Order";

    private MockMvc mockMvc;
    private MockRestServiceServer obp;

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
        ReflectionTestUtils.setField(controller, "createConsentRequestVrp", VRP_URL);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("the counterparty name the PSU typed reaches the consent request")
    void counterpartyNameIsForwarded() throws Exception {
        obp.expect(requestTo(TOKEN_ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"token-under-test\"}",
                        MediaType.APPLICATION_JSON));

        obp.expect(requestTo(VRP_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.to_account.counterparty_name").value(COUNTERPARTY_NAME))
                // Assert a neighbour too, so a body that lost every field could not pass on the
                // strength of the one under test.
                .andExpect(jsonPath("$.to_account.account_routing.address").value("to-account-address"))
                .andRespond(withSuccess("{\"consent_request_id\":\"request-under-test\"}",
                        MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/request_consents_obp_vrp")
                .session(new MockHttpSession())
                .param("bank", "test-bank")
                .param("time_to_live_in_seconds", "3600")
                .param("valid_from", "2026-08-07T10:00:00")
                .param("email", "psu@example.com")
                .param("phone_number", "+490000000000")
                .param("from_bank_routing_scheme", "OBP")
                .param("from_bank_routing_address", "test-bank")
                .param("from_routing_scheme", "OBP")
                .param("from_routing_address", "from-account-address")
                .param("to_bank_routing_scheme", "OBP")
                .param("to_bank_routing_address", "test-bank")
                .param("to_branch_routing_scheme", "")
                .param("to_branch_routing_address", "")
                .param("to_routing_scheme", "OBP")
                .param("to_routing_address", "to-account-address")
                .param("counterparty_name", COUNTERPARTY_NAME)
                .param("currency", "GBP")
                .param("max_single_amount", "100")
                .param("max_monthly_amount", "250")
                .param("max_yearly_amount", "1200")
                .param("max_number_of_monthly_transactions", "1")
                .param("max_number_of_yearly_transactions", "12")
                .param("max_total_amount", "12000")
                .param("max_number_of_transactions", "120"));

        obp.verify();
    }
}

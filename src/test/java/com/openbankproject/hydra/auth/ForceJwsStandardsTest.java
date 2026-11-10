package com.openbankproject.hydra.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** force_jws names standards whose calls Hola signs; some no longer match any call Hola makes. */
class ForceJwsStandardsTest {

    /**
     * Tests the startup warning: force_jws entries OBPv4.0.0 and OBPv3.1.0 match no call any more,
     * because Hola calls OBP-API on v5.1.0, so they are reported. BGv1.3 is still used and is not.
     */
    @Test
    @DisplayName("OBPv4.0.0 and OBPv3.1.0 are reported, because Hola calls OBP-API on v5.1.0")
    void staleOldObpVersionsAreReported() {
        // Entries are trimmed (" OBPv4.0.0") and compared ignoring case ("obpv3.1.0"), as forcesJws does;
        // each is reported as written in the setting, so the warning shows exactly what to remove.
        assertThat(RestTemplateConfig.standardsMatchingNoCall("BGv1.3, OBPv4.0.0,obpv3.1.0"))
                .containsExactly("OBPv4.0.0", "obpv3.1.0");
    }

    /** Tests that the warning stays quiet for standards Hola still calls, and when force_jws is not set. */
    @Test
    @DisplayName("standards Hola still calls, and an empty setting, report nothing")
    void currentStandardsAreNotReported() {
        // Berlin Group v1.3, OBP v5.1.0 and UK v1.3 all match calls Hola makes.
        assertThat(RestTemplateConfig.standardsMatchingNoCall("BGv1.3,OBPv5.1.0,UKv1.3")).isEmpty();
        // An empty or missing force_jws setting has nothing to report.
        assertThat(RestTemplateConfig.standardsMatchingNoCall("")).isEmpty();
        assertThat(RestTemplateConfig.standardsMatchingNoCall(null)).isEmpty();
    }

    /** Tests which request URLs force_jws signs: entries are trimmed and matched ignoring case. */
    @Test
    @DisplayName("a standard listed after a comma and a space still forces JWS")
    void entriesAreTrimmed() {
        // "OBPv5.1.0" after ", " still signs an OBP-API v5.1.0 call.
        assertThat(RestTemplateConfig.forcesJws("BGv1.3, OBPv5.1.0", "https://api.example.com/obp/v5.1.0/banks")).isTrue();
        // Spaces around the entry and lower case still sign a Berlin Group v1.3 call.
        assertThat(RestTemplateConfig.forcesJws(" bgv1.3 ", "https://api.example.com/berlin-group/v1.3/accounts")).isTrue();
        // A standard signs only its own calls: BGv1.3 does not sign an OBP-API call.
        assertThat(RestTemplateConfig.forcesJws("BGv1.3", "https://api.example.com/obp/v5.1.0/banks")).isFalse();
        // An empty setting signs nothing.
        assertThat(RestTemplateConfig.forcesJws("", "https://api.example.com/obp/v5.1.0/banks")).isFalse();
    }
}

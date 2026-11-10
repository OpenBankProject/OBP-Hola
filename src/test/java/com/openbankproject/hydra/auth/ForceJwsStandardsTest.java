package com.openbankproject.hydra.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** force_jws names standards whose calls Hola signs; some no longer match any call Hola makes. */
class ForceJwsStandardsTest {

    @Test
    @DisplayName("OBPv4.0.0 and OBPv3.1.0 are reported, because Hola calls OBP-API on v5.1.0")
    void staleOldObpVersionsAreReported() {
        assertThat(RestTemplateConfig.standardsMatchingNoCall("BGv1.3, OBPv4.0.0,obpv3.1.0"))
                .containsExactly("OBPv4.0.0", "obpv3.1.0");
    }

    @Test
    @DisplayName("standards Hola still calls, and an empty setting, report nothing")
    void currentStandardsAreNotReported() {
        assertThat(RestTemplateConfig.standardsMatchingNoCall("BGv1.3,OBPv5.1.0,UKv1.3")).isEmpty();
        assertThat(RestTemplateConfig.standardsMatchingNoCall("")).isEmpty();
        assertThat(RestTemplateConfig.standardsMatchingNoCall(null)).isEmpty();
    }
}

package com.openbankproject.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /alive answers like OBP-API's: 200, application/json, the body true, and never cached.
 */
class AliveControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AliveController()).build();

    @Test
    @DisplayName("GET /alive answers 200 with the JSON body true")
    void aliveAnswersTrue() throws Exception {
        mockMvc.perform(get("/alive"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string("true"));
    }
}

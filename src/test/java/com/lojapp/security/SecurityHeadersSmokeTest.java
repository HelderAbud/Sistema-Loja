package com.lojapp.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityHeadersSmokeTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void health_includesBaselineSecurityHeaders() throws Exception {
        MvcResult result =
                mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andReturn();

        assertThat(result.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(result.getResponse().getHeader("X-Frame-Options")).isIn("SAMEORIGIN", "DENY");
        assertThat(result.getResponse().getHeader("Referrer-Policy"))
                .isEqualTo("strict-origin-when-cross-origin");
    }
}

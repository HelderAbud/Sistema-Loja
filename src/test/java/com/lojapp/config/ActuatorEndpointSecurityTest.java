package com.lojapp.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Com {@code lojapp.security.actuator-metrics-anonymous=false} (ver {@code src/test/resources/application.yml}),
 * mÃ©tricas nÃ£o podem ser lidas sem autenticaÃ§Ã£o.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void actuatorHealth_isPermittedWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void actuatorPrometheus_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorMetrics_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
    }
}

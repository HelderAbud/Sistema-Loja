package com.lojapp.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RequestCorrelationIntegrationTest {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Autowired private MockMvc mockMvc;

    @Test
    void health_withIncomingRequestId_echoesSameResponseHeader() throws Exception {
        mockMvc.perform(get("/actuator/health").header(REQUEST_ID_HEADER, "req-test-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(REQUEST_ID_HEADER, "req-test-123"));
    }

    @Test
    void health_withoutIncomingRequestId_generatesResponseHeader() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists(REQUEST_ID_HEADER));
    }
}

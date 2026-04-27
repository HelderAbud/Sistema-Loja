package com.lojapp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void whenTrustDisabled_ignoresForwardedFor() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 198.51.100.1");

        assertThat(ClientIpResolver.primaryClientIp(req, false)).isEqualTo("10.0.0.1");
    }

    @Test
    void whenTrustEnabled_usesFirstForwardedClient() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.9, 198.51.100.1");

        assertThat(ClientIpResolver.primaryClientIp(req, true)).isEqualTo("203.0.113.9");
    }

    @Test
    void whenTrustEnabledButHeaderMissing_fallsBackToRemoteAddr() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("192.168.2.2");

        assertThat(ClientIpResolver.primaryClientIp(req, true)).isEqualTo("192.168.2.2");
    }
}

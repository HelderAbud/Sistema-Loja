package com.lojapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.config.AuthRegistrationProperties;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {

    @Test
    void register_requestsBeyondHourlyCap_return429() throws Exception {
        AuthRegistrationProperties props = new AuthRegistrationProperties(true, "", 5, null);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false, "memory", props, null, Clock.systemUTC());

        List<Integer> statuses = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest();
            req.setMethod("POST");
            req.setRequestURI("/api/v1/auth/register");
            req.setRemoteAddr("192.168.55.44");
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = new MockFilterChain();
            filter.doFilter(req, res, chain);
            statuses.add(res.getStatus());
        }

        assertThat(statuses.subList(0, 5)).doesNotContain(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(statuses.get(5)).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void login_requestsWithinCap_proceed() throws Exception {
        AuthRegistrationProperties props = new AuthRegistrationProperties(true, "", 10, null);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false, "memory", props, null, Clock.systemUTC());

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/api/v1/auth/login");
        req.setRemoteAddr("10.0.0.9");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();
        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void redisMode_withoutRedisClient_fallsBackToLocalLimiter() throws Exception {
        AuthRegistrationProperties props = new AuthRegistrationProperties(true, "", 1, null);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(false, "redis", props, null, Clock.systemUTC());

        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setMethod("POST");
        req1.setRequestURI("/api/v1/auth/register");
        req1.setRemoteAddr("172.16.0.1");
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        filter.doFilter(req1, res1, new MockFilterChain());

        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setMethod("POST");
        req2.setRequestURI("/api/v1/auth/register");
        req2.setRemoteAddr("172.16.0.1");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req2, res2, new MockFilterChain());

        assertThat(res1.getStatus()).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(res2.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }
}

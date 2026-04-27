package com.lojapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthCsrfGuardFilterTest {

    private static final String ORIGINS = "http://localhost:3000,http://127.0.0.1:3000";

    @Test
    void refresh_withRefreshCookieAndUntrustedOrigin_returns403() throws Exception {
        AuthCsrfGuardFilter filter = new AuthCsrfGuardFilter(ORIGINS, "lojapp_rt");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/api/v1/auth/refresh");
        req.setCookies(new Cookie("lojapp_rt", "token"));
        req.addHeader("Origin", "http://evil.local");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(403);
    }

    @Test
    void refresh_withRefreshCookieAndAllowedOrigin_passes() throws Exception {
        AuthCsrfGuardFilter filter = new AuthCsrfGuardFilter(ORIGINS, "lojapp_rt");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/api/v1/auth/refresh");
        req.setCookies(new Cookie("lojapp_rt", "token"));
        req.addHeader("Origin", "http://localhost:3000");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(403);
    }

    @Test
    void refresh_withoutRefreshCookie_doesNotApplyCsrfBlock() throws Exception {
        AuthCsrfGuardFilter filter = new AuthCsrfGuardFilter(ORIGINS, "lojapp_rt");
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setMethod("POST");
        req.setRequestURI("/api/v1/auth/refresh");
        req.addHeader("Origin", "http://evil.local");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isNotEqualTo(403);
    }
}

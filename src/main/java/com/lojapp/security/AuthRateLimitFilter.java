package com.lojapp.security;

import com.lojapp.config.AuthRegistrationProperties;
import com.lojapp.util.ClientIpResolver;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Limita tentativas de login/refresh por IP (burst por minuto) e registo por IP (por hora).
 * Opcionalmente usa {@code X-Forwarded-For} quando {@code lojapp.security.trust-forward-headers=true}
 * (atrás de reverse proxy de confiança).
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);

    private static final int LOGIN_REFRESH_CAPACITY_PER_MINUTE = 60;

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private final Cache<String, Bucket> loginRefreshBuckets;
    private final Cache<String, Bucket> registerBuckets;

    private final boolean trustForwardHeaders;
    private final AuthRegistrationProperties registrationProperties;
    private final boolean distributedRedisMode;
    private final StringRedisTemplate redis;
    private final Clock clock;
    private final AtomicBoolean redisFallbackWarned = new AtomicBoolean(false);

    @Autowired
    public AuthRateLimitFilter(
            @Value("${lojapp.security.trust-forward-headers:false}") boolean trustForwardHeaders,
            @Value("${lojapp.security.rate-limit-mode:memory}") String rateLimitMode,
            AuthRegistrationProperties registrationProperties,
            StringRedisTemplate redis) {
        this.trustForwardHeaders = trustForwardHeaders;
        this.distributedRedisMode = "redis".equalsIgnoreCase(rateLimitMode);
        this.registrationProperties = registrationProperties;
        this.redis = redis;
        this.clock = Clock.systemUTC();
        this.loginRefreshBuckets = buildInMemoryCache(10, TimeUnit.MINUTES);
        this.registerBuckets = buildInMemoryCache(2, TimeUnit.HOURS);
    }

    AuthRateLimitFilter(
            boolean trustForwardHeaders,
            String rateLimitMode,
            AuthRegistrationProperties registrationProperties,
            StringRedisTemplate redis,
            Clock clock) {
        this.trustForwardHeaders = trustForwardHeaders;
        this.distributedRedisMode = "redis".equalsIgnoreCase(rateLimitMode);
        this.registrationProperties = registrationProperties;
        this.redis = redis;
        this.clock = clock;
        this.loginRefreshBuckets = buildInMemoryCache(10, TimeUnit.MINUTES);
        this.registerBuckets = buildInMemoryCache(2, TimeUnit.HOURS);
    }

    private static Cache<String, Bucket> buildInMemoryCache(long ttl, TimeUnit unit) {
        return Caffeine.newBuilder().maximumSize(20_000).expireAfterAccess(ttl, unit).build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        if (uri == null) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = ClientIpResolver.primaryClientIp(request, trustForwardHeaders);

        if (uri.endsWith("/api/v1/auth/register")) {
            if (!consumeRegistration(key)) {
                denyTooManyRegistrations(response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (uri.endsWith("/api/v1/auth/login") || uri.endsWith("/api/v1/auth/refresh")) {
            if (!consumeLoginRefresh(key)) {
                denyTooManyAuthAttempts(response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static void denyTooManyAuthAttempts(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Muitas tentativas. Tente novamente dentro de um minuto.");
    }

    private static void denyTooManyRegistrations(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "3600");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Limite de registos por hora excedido. Tente mais tarde.");
    }

    private static Bucket newLoginRefreshPerMinuteBucket() {
        return Bucket.builder()
                .addLimit(
                        Bandwidth.classic(
                                LOGIN_REFRESH_CAPACITY_PER_MINUTE,
                                Refill.intervally(
                                        LOGIN_REFRESH_CAPACITY_PER_MINUTE, Duration.ofMinutes(1))))
                .build();
    }

    private static Bucket newRegisterPerHourBucket(int maxPerHour) {
        return Bucket.builder()
                .addLimit(
                        Bandwidth.classic(
                                maxPerHour, Refill.intervally(maxPerHour, Duration.ofHours(1))))
                .build();
    }

    private boolean consumeRegistration(String clientKey) {
        int max = registrationProperties.maxPerIpPerHour();
        return tryConsume(
                "auth-register",
                clientKey,
                max,
                Duration.ofHours(1),
                registerBuckets,
                () -> newRegisterPerHourBucket(max));
    }

    private boolean consumeLoginRefresh(String clientKey) {
        return tryConsume(
                "auth-login-refresh",
                clientKey,
                LOGIN_REFRESH_CAPACITY_PER_MINUTE,
                Duration.ofMinutes(1),
                loginRefreshBuckets,
                AuthRateLimitFilter::newLoginRefreshPerMinuteBucket);
    }

    private boolean tryConsume(
            String scope,
            String clientKey,
            int capacity,
            Duration window,
            Cache<String, Bucket> fallbackCache,
            Supplier<Bucket> fallbackFactory) {
        if (distributedRedisMode) {
            if (redis == null) {
                warnRedisFallback(scope, "cliente Redis indisponível (bean ausente)", null);
            } else {
                try {
                    return tryConsumeRedis(scope, clientKey, capacity, window);
                } catch (RuntimeException ex) {
                    warnRedisFallback(scope, "erro ao usar Redis", ex);
                }
            }
        }
        Bucket bucket = fallbackCache.get(clientKey, ignored -> fallbackFactory.get());
        return bucket.tryConsume(1);
    }

    private boolean tryConsumeRedis(String scope, String clientKey, int capacity, Duration window) {
        long epochWindow = Instant.now(clock).getEpochSecond() / window.getSeconds();
        String key = "lojapp:ratelimit:%s:%s:%d".formatted(scope, clientKey, epochWindow);
        Long count = redis.opsForValue().increment(key);
        if (count == null) {
            throw new IllegalStateException("Falha ao incrementar contador de rate limit no Redis");
        }
        if (count == 1L) {
            redis.expire(key, window);
        }
        return count <= capacity;
    }

    private void warnRedisFallback(String scope, String reason, RuntimeException ex) {
        if (redisFallbackWarned.compareAndSet(false, true)) {
            if (ex == null) {
                log.warn(
                        "Rate limit redis indisponivel ({}); fallback para memoria na instancia atual. scope={}",
                        reason,
                        scope);
            } else {
                log.warn(
                        "Rate limit redis indisponivel ({}); fallback para memoria na instancia atual. scope={}",
                        reason,
                        scope,
                        ex);
            }
        }
    }
}

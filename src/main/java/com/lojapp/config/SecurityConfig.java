package com.lojapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.ApiErrorResponse;
import com.lojapp.security.AuthRateLimitFilter;
import com.lojapp.security.AuthCsrfGuardFilter;
import com.lojapp.security.ApiAccessLogFilter;
import com.lojapp.security.JwtAuthFilter;
import com.lojapp.security.RequestCorrelationFilter;
import java.time.Instant;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthCsrfGuardFilter authCsrfGuardFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final RequestCorrelationFilter requestCorrelationFilter;
    private final ApiAccessLogFilter apiAccessLogFilter;
    private final ObjectMapper objectMapper;
    private final boolean apiDocsEnabled;
    /**
     * Se {@code false} (recomendado em produção), {@code /actuator/prometheus} e
     * {@code /actuator/metrics} exigem JWT. Se {@code true}, qualquer cliente pode ler métricas
     * (útil só em rede local de desenvolvimento com Prometheus sem auth).
     */
    private final boolean actuatorMetricsAnonymous;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            AuthCsrfGuardFilter authCsrfGuardFilter,
            AuthRateLimitFilter authRateLimitFilter,
            RequestCorrelationFilter requestCorrelationFilter,
            ApiAccessLogFilter apiAccessLogFilter,
            ObjectMapper objectMapper,
            @Value("${springdoc.api-docs.enabled:true}") boolean apiDocsEnabled,
            @Value("${lojapp.security.actuator-metrics-anonymous:false}")
                    boolean actuatorMetricsAnonymous) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authCsrfGuardFilter = authCsrfGuardFilter;
        this.authRateLimitFilter = authRateLimitFilter;
        this.requestCorrelationFilter = requestCorrelationFilter;
        this.apiAccessLogFilter = apiAccessLogFilter;
        this.objectMapper = objectMapper;
        this.apiDocsEnabled = apiDocsEnabled;
        this.actuatorMetricsAnonymous = actuatorMetricsAnonymous;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                // Sem isto, o Boot pode expor Basic Auth + utilizador gerado; o login JWT fica à sombra.
                .httpBasic(b -> b.disable())
                .formLogin(f -> f.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                        (request, response, ex) -> {
                                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                            ApiErrorResponse payload =
                                                    new ApiErrorResponse(
                                                            "Não autenticado",
                                                            ApiErrorCode.UNAUTHORIZED.code(),
                                                            HttpStatus.UNAUTHORIZED.value(),
                                                            request.getRequestURI(),
                                                            Instant.now());
                                            objectMapper.writeValue(
                                                    response.getOutputStream(), payload);
                                        }))
                .authorizeHttpRequests(
                        auth -> {
                            // Preflight CORS (necessário se o browser falar com a API noutra origem/porta).
                            auth.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll();
                            auth.requestMatchers(HttpMethod.POST, "/api/v1/auth/**")
                                    .permitAll();
                            auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                            auth.requestMatchers(HttpMethod.GET, "/actuator/info").permitAll();
                            if (actuatorMetricsAnonymous) {
                                auth.requestMatchers(HttpMethod.GET, "/actuator/prometheus")
                                        .permitAll();
                                auth.requestMatchers(HttpMethod.GET, "/actuator/metrics/**")
                                        .permitAll();
                            }
                            if (apiDocsEnabled) {
                                auth.requestMatchers(
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll();
                            }
                            auth.anyRequest().authenticated();
                        })
                // Não ancorar em JwtAuthFilter: em @WebMvcTest o filtro é mock e não tem ordem na cadeia.
                .addFilterBefore(requestCorrelationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authCsrfGuardFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(apiAccessLogFilter, JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilterRegistration(
            AuthRateLimitFilter filter) {
        FilterRegistrationBean<AuthRateLimitFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AuthCsrfGuardFilter> authCsrfGuardFilterRegistration(
            AuthCsrfGuardFilter filter) {
        FilterRegistrationBean<AuthCsrfGuardFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilterRegistration(
            RequestCorrelationFilter filter) {
        FilterRegistrationBean<RequestCorrelationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<ApiAccessLogFilter> apiAccessLogFilterRegistration(
            ApiAccessLogFilter filter) {
        FilterRegistrationBean<ApiAccessLogFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}

package com.lojapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lojapp.jwt")
public record JwtProperties(String secret, long expirationMs, long refreshExpirationMs) {}

package com.lojapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lojapp.idempotency")
public record IdempotencyProperties(int ttlHours, int maxKeyLength) {}

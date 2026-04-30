package com.lojapp.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lojapp.pos")
public record PosProperties(BigDecimal closeToleranceAmount) {

    public PosProperties {
        if (closeToleranceAmount == null) {
            closeToleranceAmount = new BigDecimal("10.00");
        }
    }
}

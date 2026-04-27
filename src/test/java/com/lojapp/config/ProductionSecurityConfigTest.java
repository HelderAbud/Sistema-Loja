package com.lojapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.NestedExceptionUtils;

class ProductionSecurityConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(JwtConfig.class, ProductionSecurityConfig.class)
                    .withPropertyValues(
                            "spring.profiles.active=prod", "lojapp.jwt.expiration-ms=86400000");

    @Test
    void prodProfile_rejectsDevelopmentJwtPlaceholder() {
        runner.withPropertyValues(
                        "lojapp.jwt.secret=dev-only-change-this-secret-min-32-chars-long!!")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            Throwable root =
                                    NestedExceptionUtils.getMostSpecificCause(
                                            context.getStartupFailure());
                            assertThat(root)
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessageContaining("LOJAPP_JWT_SECRET");
                        });
    }

    @Test
    void prodProfile_rejectsTooShortJwtSecret() {
        runner.withPropertyValues("lojapp.jwt.secret=short-secret")
                .run(
                        context -> {
                            assertThat(context).hasFailed();
                            Throwable root =
                                    NestedExceptionUtils.getMostSpecificCause(
                                            context.getStartupFailure());
                            assertThat(root)
                                    .isInstanceOf(IllegalStateException.class)
                                    .hasMessageContaining("LOJAPP_JWT_SECRET")
                                    .hasMessageContaining("curto");
                        });
    }

    @Test
    void prodProfile_acceptsStrongJwtSecret() {
        runner.withPropertyValues(
                        "lojapp.jwt.secret=integration-test-secret-32-chars-min!!")
                .run(context -> assertThat(context).hasNotFailed());
    }
}

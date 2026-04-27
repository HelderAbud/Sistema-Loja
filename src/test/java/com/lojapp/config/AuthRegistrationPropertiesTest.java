package com.lojapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthRegistrationPropertiesTest {

    @Test
    void allowedDomainSet_trimsAndLowercasesCsv() {
        AuthRegistrationProperties p = new AuthRegistrationProperties(true, " A.B , c.d ,", 10, null);
        assertThat(p.allowedDomainSet()).containsExactlyInAnyOrder("a.b", "c.d");
    }

    @Test
    void maxPerIpPerHour_belowOne_clampedToOne() {
        AuthRegistrationProperties p = new AuthRegistrationProperties(true, "", 0, null);
        assertThat(p.maxPerIpPerHour()).isEqualTo(1);
    }

    @Test
    void inviteSecretConfigured_whenBlank_false() {
        AuthRegistrationProperties p = new AuthRegistrationProperties(true, "", 10, "  ");
        assertThat(p.inviteSecretConfigured()).isFalse();
    }

    @Test
    void inviteSecretConfigured_whenSet_true() {
        AuthRegistrationProperties p = new AuthRegistrationProperties(true, "", 10, "x");
        assertThat(p.inviteSecretConfigured()).isTrue();
    }
}

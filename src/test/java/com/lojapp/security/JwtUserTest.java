package com.lojapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JwtUserTest {

    @ParameterizedTest
    @CsvSource({
        "USER, ROLE_USER",
        "ADMIN, ROLE_ADMIN",
        "REPRESENTATIVE, ROLE_REPRESENTATIVE",
        "CASHIER, ROLE_CASHIER",
        "SELLER, ROLE_SELLER",
        "MANAGER, ROLE_MANAGER"
    })
    void authorities_containsOnlyExactRole(String storedRole, String expectedAuthority) {
        JwtUser user = new JwtUser(1L, "u@test", storedRole);

        Set<String> authorities = user.authorities();

        assertThat(authorities).containsExactly(expectedAuthority);
    }

    @Test
    void authorities_managerDoesNotGrantAdmin() {
        JwtUser manager = new JwtUser(2L, "m@test", "MANAGER");

        assertThat(manager.authorities()).doesNotContain("ROLE_ADMIN");
    }

    @Test
    void authorities_userDoesNotGrantCashier() {
        JwtUser user = new JwtUser(3L, "u@test", "USER");

        assertThat(user.authorities()).doesNotContain("ROLE_CASHIER");
    }
}

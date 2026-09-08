package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.entity.User;
import com.lojapp.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AuthListUsersForAdminUseCaseTest {

    @Mock private UserRepository users;

    private AuthListUsersForAdminUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthListUsersForAdminUseCase(users);
    }

    @Test
    void execute_returnsOnlyTheAuthenticatedAdmin_notOtherTenants() {
        User admin = user(11L, "admin@lojapp.test", "ADMIN");
        when(users.findById(11L)).thenReturn(Optional.of(admin));
        Pageable pageable = PageRequest.of(0, 20);

        var page = useCase.execute(11L, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(11L);
        assertThat(page.getContent().get(0).email()).isEqualTo("admin@lojapp.test");
        verify(users).findById(11L);
        verify(users, never()).findAll(any(Pageable.class));
    }

    @Test
    void execute_whenActorMissing_returnsEmptyPage() {
        when(users.findById(99L)).thenReturn(Optional.empty());

        var page = useCase.execute(99L, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
        verify(users, never()).findAll(any(Pageable.class));
    }

    private static User user(long id, String email, String role) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setAppRole(role);
        return u;
    }
}

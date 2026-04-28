package com.lojapp.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import com.lojapp.entity.User;
import com.lojapp.repository.UserRepository;
import com.lojapp.security.JwtService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserAuthorizationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void adminList_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/admin/list"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/users/admin/list"))
                .andExpect(jsonPath("$.message").value("Não autenticado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void adminList_withUserRole_returns403() throws Exception {
        User user = createUser("USER");
        String userToken = jwtService.createToken(user.getId(), user.getEmail(), user.getAppRole());

        mockMvc.perform(
                        get("/api/v1/users/admin/list")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/users/admin/list"))
                .andExpect(jsonPath("$.message").value("Acesso negado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void adminList_withAdminRole_returns200AndPagePayload() throws Exception {
        User admin = createUser("ADMIN");
        createUser("USER");
        String adminToken = jwtService.createToken(admin.getId(), admin.getEmail(), admin.getAppRole());

        mockMvc.perform(
                        get("/api/v1/users/admin/list")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)));
    }

    private User createUser(String role) {
        User user = new User();
        user.setEmail("authz-" + role.toLowerCase() + "-" + UUID.randomUUID() + "@lojapp.test");
        user.setPasswordHash(passwordEncoder.encode("senha1234"));
        user.setAppRole(role);
        return userRepository.save(user);
    }
}

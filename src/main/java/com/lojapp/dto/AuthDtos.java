package com.lojapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @Schema(
                            description =
                                    "Obrigatório quando lojapp.auth.registration.invite-secret está definido; partilhar o valor só por canal seguro.")
                    String inviteToken) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    /** Corpo opcional: o refresh pode vir só da cookie HttpOnly {@code lojapp_rt}. */
    public record RefreshRequest(String refreshToken) {}

    @Schema(description = "JWT de acesso. O refresh opaco é enviado em cookie HttpOnly (não aparece aqui).")
    public record AccessTokenResponse(String accessToken) {}

    /** Resultado interno login/registo/refresh (antes de mapear para cookie + corpo JSON). */
    public record IssuedAuthTokens(String accessToken, String rawRefreshToken) {}

    @Schema(description = "Perfil do utilizador autenticado")
    public record UserMeResponse(
            long id,
            @Schema(description = "Email da conta") String email,
            @Schema(
                            description =
                                    "Papel da aplicação: USER, ADMIN ou REPRESENTATIVE (B2B — mesmo isolamento por user_id)")
                    String appRole) {}
}

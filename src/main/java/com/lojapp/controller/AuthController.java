package com.lojapp.controller;

import com.lojapp.config.AuthRefreshCookieSupport;
import com.lojapp.dto.AuthDtos.AccessTokenResponse;
import com.lojapp.dto.AuthDtos.LoginRequest;
import com.lojapp.dto.AuthDtos.RefreshRequest;
import com.lojapp.dto.AuthDtos.RegisterRequest;
import com.lojapp.service.contract.AuthServiceContract;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthServiceContract authService;
    private final AuthRefreshCookieSupport refreshCookie;

    public AuthController(AuthServiceContract authService, AuthRefreshCookieSupport refreshCookie) {
        this.authService = authService;
        this.refreshCookie = refreshCookie;
    }

    @PostMapping("/register")
    @Operation(
            summary = "Registo de utilizador",
            description =
                    "Cria conta. O access JWT vem no JSON; o refresh opaco é gravado em cookie HttpOnly (path /api/v1/auth).")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Conta criada",
                content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido ou validação falhou"),
        @ApiResponse(
                responseCode = "403",
                description =
                        "Registo desativado, domínio de email não autorizado ou convite (inviteToken) inválido"),
        @ApiResponse(responseCode = "409", description = "Email já cadastrado"),
        @ApiResponse(
                responseCode = "429",
                description = "Limite de tentativas de registo por IP (por hora) excedido")
    })
    public ResponseEntity<AccessTokenResponse> register(
            @Valid @RequestBody RegisterRequest body, HttpServletResponse response) {
        var issued = authService.register(body);
        refreshCookie.writeRefreshCookie(response, issued.rawRefreshToken());
        return ResponseEntity.ok(new AccessTokenResponse(issued.accessToken()));
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login",
            description =
                    "Autentica com email e palavra-passe. Access JWT no JSON; refresh em cookie HttpOnly.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Autenticação bem-sucedida",
                content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "Corpo inválido ou validação falhou"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest body, HttpServletResponse response) {
        var issued = authService.login(body);
        refreshCookie.writeRefreshCookie(response, issued.rawRefreshToken());
        return ResponseEntity.ok(new AccessTokenResponse(issued.accessToken()));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar access token",
            description =
                    "Lê o refresh da cookie HttpOnly (ou do corpo JSON para compatibilidade). Rotação: emite nova cookie e novo access.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "Access renovado",
                content = @Content(schema = @Schema(implementation = AccessTokenResponse.class))),
        @ApiResponse(responseCode = "400", description = "Refresh ausente"),
        @ApiResponse(responseCode = "401", description = "Refresh inválido ou expirado")
    })
    public ResponseEntity<AccessTokenResponse> refresh(
            @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {
        String raw = resolveRefreshToken(body, request);
        if (raw.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token ausente");
        }
        var issued = authService.refresh(raw);
        refreshCookie.writeRefreshCookie(response, issued.rawRefreshToken());
        return ResponseEntity.ok(new AccessTokenResponse(issued.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Terminar sessão no browser",
            description = "Remove a cookie de refresh (HttpOnly). O access JWT deve ser descartado no cliente.")
    @ApiResponse(responseCode = "204", description = "Cookie de refresh limpa")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(readCookieValue(request, refreshCookie.cookieName()));
        refreshCookie.clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String resolveRefreshToken(RefreshRequest body, HttpServletRequest request) {
        String fromBody = body != null && body.refreshToken() != null ? body.refreshToken().trim() : "";
        String fromCookie = readCookieValue(request, refreshCookie.cookieName());
        if (!fromBody.isEmpty() && !fromCookie.isEmpty() && !fromBody.equals(fromCookie)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inconsistente");
        }
        return !fromCookie.isEmpty() ? fromCookie : fromBody;
    }

    private static String readCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName()) && c.getValue() != null) {
                return c.getValue().trim();
            }
        }
        return "";
    }
}

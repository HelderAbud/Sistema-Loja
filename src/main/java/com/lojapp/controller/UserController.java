package com.lojapp.controller;

import com.lojapp.dto.AuthDtos.UserMeResponse;
import com.lojapp.dto.user.AdminUserPageResponse;
import com.lojapp.security.JwtUser;
import com.lojapp.service.contract.AuthServiceContract;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class UserController {

    private final AuthServiceContract authService;

    public UserController(AuthServiceContract authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public UserMeResponse me(@AuthenticationPrincipal JwtUser principal) {
        return authService.me(principal.userId());
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar utilizadores (somente ADMIN)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de utilizadores"),
        @ApiResponse(responseCode = "403", description = "Acesso restrito a ADMIN")
    })
    public AdminUserPageResponse listUsersForAdmin(
            @ParameterObject
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        return AdminUserPageResponse.from(authService.listUsersForAdmin(pageable));
    }
}

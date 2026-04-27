package com.lojapp.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(
                        new Info()
                                .title("LojApp Pro API")
                                .description(
                                        "MVP NFe + estoque + marca. **Listagem de produtos** "
                                                + "(`GET /api/v1/lojapp/products`): resposta paginada em objeto "
                                                + "com `content`, `totalElements`, etc.  não é um array JSON na "
                                                + "raiz. Erros: JSON `ApiErrorResponse` com `message`, `code` e "
                                                + "`timestamp` (ISO-8601). Códigos fixos incluem `VALIDATION_ERROR`, "
                                                + "`BAD_REQUEST`, `INTERNAL_ERROR`, ou "
                                                + "o nome do estado HTTP (ex. `CONFLICT`, `NOT_FOUND`) em "
                                                + "respostas com `LojappDomainException` / "
                                                + "`ResponseStatusException`.")
                                .version("v1"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        scheme,
                                        new SecurityScheme()
                                                .name(scheme)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}

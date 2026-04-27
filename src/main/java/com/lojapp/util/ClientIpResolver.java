package com.lojapp.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolve o IP do cliente para rate limiting / auditoria.
 *
 * <p>{@code X-Forwarded-For} só deve ser considerado quando a API corre atrás de um reverse proxy
 * que <strong>remove ou substitui</strong> esse cabeçalho de pedidos externos (caso contrário um
 * atacante pode forjar o IP e contornar limites por IP).
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    /**
     * @param trustForwardedFor se {@code true}, usa o primeiro valor de {@code X-Forwarded-For}
     *     quando presente; caso contrário usa apenas {@link HttpServletRequest#getRemoteAddr()}.
     */
    public static String primaryClientIp(HttpServletRequest request, boolean trustForwardedFor) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

package com.lojapp.util;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import java.util.Optional;

/** Normalização de CNPJ/CPF (somente dígitos) para fornecedores e NFe. */
public final class TaxIdNormalizer {

    private TaxIdNormalizer() {}

    /**
     * Valor a gravar: vazio  empty; senão 11 ou 14 dígitos após remover máscara.
     *
     * @throws LojappDomainException se houver texto não vazio mas quantidade de dígitos inválida
     */
    public static Optional<String> forStorage(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        if (digits.length() > 14) {
            digits = digits.substring(0, 14);
        }
        if (digits.length() != 11 && digits.length() != 14) {
            throw new LojappDomainException(
                    ApiErrorCode.BAD_REQUEST, "CNPJ ou CPF deve ter 11 ou 14 dígitos");
        }
        return Optional.of(digits);
    }
}

package com.lojapp.util;

import java.util.Optional;

/** Normalização de GTIN/EAN para match na importação de NFe e armazenamento consistente. */
public final class EanNormalizer {

    private static final int MIN_GTIN_DIGITS = 8;

    private EanNormalizer() {}

    /** EAN utilizável para lookup: só dígitos, comprimento mínimo típico de GTIN. */
    public static Optional<String> forLookup(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("SEM GTIN")) {
            return Optional.empty();
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() < MIN_GTIN_DIGITS) {
            return Optional.empty();
        }
        return Optional.of(digits);
    }

    /** Valor a gravar no produto (null se inválido ou vazio). */
    public static String forStorage(String raw) {
        return forLookup(raw).orElse(null);
    }
}

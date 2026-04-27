package com.lojapp.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Heurística leve: se o nome de uma marca da loja aparece como substring (sem distinguir maiúsculas)
 * no emitente ou nas descrições dos itens, sugere essa marca (prioriza nomes mais longos).
 */
public final class NfeBrandSuggester {

    /** Evita ruído com marcas de 12 caracteres. */
    private static final int MIN_BRAND_NAME_LENGTH = 3;

    public record BrandCandidate(long id, String name) {}

    private NfeBrandSuggester() {}

    public static Optional<BrandCandidate> suggest(
            List<BrandCandidate> brands, String supplierName, List<String> itemDescriptions) {
        if (brands == null || brands.isEmpty()) {
            return Optional.empty();
        }
        List<BrandCandidate> sorted = new ArrayList<>(brands);
        sorted.sort(
                Comparator.comparingInt((BrandCandidate b) -> b.name().length()).reversed());

        String haystack = buildHaystack(supplierName, itemDescriptions);
        if (haystack.isBlank()) {
            return Optional.empty();
        }
        String hayLower = haystack.toLowerCase(Locale.ROOT);
        for (BrandCandidate c : sorted) {
            String n = c.name().trim();
            if (n.length() < MIN_BRAND_NAME_LENGTH) {
                continue;
            }
            if (hayLower.contains(n.toLowerCase(Locale.ROOT))) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private static String buildHaystack(String supplierName, List<String> itemDescriptions) {
        StringBuilder sb = new StringBuilder();
        if (supplierName != null) {
            sb.append(supplierName).append(' ');
        }
        if (itemDescriptions != null) {
            for (String d : itemDescriptions) {
                if (d != null) {
                    sb.append(d).append(' ');
                }
            }
        }
        return sb.toString().trim();
    }
}

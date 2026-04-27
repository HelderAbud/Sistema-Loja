package com.lojapp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EanNormalizerTest {

    @Test
    void forLookup_stripsNonDigits() {
        assertThat(EanNormalizer.forLookup("789 12345-67890")).contains("7891234567890");
    }

    @Test
    void forLookup_semGtin_returnsEmpty() {
        assertThat(EanNormalizer.forLookup("SEM GTIN")).isEmpty();
        assertThat(EanNormalizer.forLookup("sem gtin")).isEmpty();
    }

    @Test
    void forLookup_blankOrNull_returnsEmpty() {
        assertThat(EanNormalizer.forLookup(null)).isEmpty();
        assertThat(EanNormalizer.forLookup("")).isEmpty();
        assertThat(EanNormalizer.forLookup("   ")).isEmpty();
    }

    @Test
    void forLookup_tooFewDigits_returnsEmpty() {
        assertThat(EanNormalizer.forLookup("1234567")).isEmpty();
    }

    @Test
    void forLookup_ean8_returnsDigits() {
        assertThat(EanNormalizer.forLookup("12345678")).contains("12345678");
    }

    @Test
    void forStorage_invalid_returnsNull() {
        assertThat(EanNormalizer.forStorage("SEM GTIN")).isNull();
        assertThat(EanNormalizer.forStorage("bad")).isNull();
    }

    @Test
    void forStorage_valid_returnsDigitsOnly() {
        assertThat(EanNormalizer.forStorage("789 1234567890")).isEqualTo("7891234567890");
    }
}

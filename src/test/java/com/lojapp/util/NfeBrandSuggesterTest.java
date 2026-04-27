package com.lojapp.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.util.NfeBrandSuggester.BrandCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NfeBrandSuggesterTest {

    @Test
    void suggest_prefersLongerBrandName() {
        var brands =
                List.of(
                        new BrandCandidate(1L, "Nike"),
                        new BrandCandidate(2L, "Nike Sport"),
                        new BrandCandidate(3L, "ZZ"));
        assertThat(
                        NfeBrandSuggester.suggest(
                                brands,
                                "Distribuidora",
                                List.of("Camisa Polo Nike Sport")))
                .contains(new BrandCandidate(2L, "Nike Sport"));
    }

    @Test
    void suggest_ignoresVeryShortBrandNames() {
        var brands = List.of(new BrandCandidate(1L, "AB"));
        assertThat(NfeBrandSuggester.suggest(brands, "AB Nike CD", List.of("X"))).isEmpty();
    }
}

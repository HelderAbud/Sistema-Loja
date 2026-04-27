package com.lojapp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TaxIdNormalizerTest {

    @Test
    void forStorage_masksCnpj() {
        assertThat(TaxIdNormalizer.forStorage("12.345.678/0001-99")).contains("12345678000199");
    }

    @Test
    void forStorage_blank_returnsEmpty() {
        assertThat(TaxIdNormalizer.forStorage("  ")).isEmpty();
    }

    @Test
    void forStorage_invalidDigitCount_throwsBadRequest() {
        assertThatThrownBy(() -> TaxIdNormalizer.forStorage("123456789012"))
                .isInstanceOfSatisfying(
                        LojappDomainException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.BAD_REQUEST));
    }
}

package com.lojapp.domain.nfe;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NfeLastPurchaseCostTest {

    @Test
    void replacementCost_whenNfeDiffers_returnsXmlUnitCost() {
        assertThat(
                        NfeLastPurchaseCost.replacementCost(
                                new BigDecimal("10.00"), new BigDecimal("20.00")))
                .contains(new BigDecimal("20.00"));
    }

    @Test
    void replacementCost_whenSame_isEmpty() {
        assertThat(
                        NfeLastPurchaseCost.replacementCost(
                                new BigDecimal("15.00"), new BigDecimal("15.00")))
                .isEmpty();
    }

    @Test
    void replacementCost_zeroGift_overwritesPreviousCost() {
        assertThat(NfeLastPurchaseCost.replacementCost(new BigDecimal("8.00"), BigDecimal.ZERO))
                .contains(BigDecimal.ZERO);
    }

    @Test
    void replacementCost_nullXml_isEmpty() {
        assertThat(NfeLastPurchaseCost.replacementCost(new BigDecimal("8.00"), null)).isEmpty();
    }
}

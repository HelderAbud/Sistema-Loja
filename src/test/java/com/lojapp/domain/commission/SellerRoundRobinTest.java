package com.lojapp.domain.commission;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SellerRoundRobinTest {

    @Test
    void nextId_empty_returnsNull() {
        assertThat(SellerRoundRobin.nextId(List.of(), null)).isNull();
    }

    @Test
    void nextId_firstSale_picksFirst() {
        assertThat(SellerRoundRobin.nextId(List.of(10L, 20L, 30L), null)).isEqualTo(10L);
    }

    @Test
    void nextId_wrapsAfterLast() {
        assertThat(SellerRoundRobin.nextId(List.of(10L, 20L, 30L), 30L)).isEqualTo(10L);
    }

    @Test
    void nextId_unknownLast_startsAtFirst() {
        assertThat(SellerRoundRobin.nextId(List.of(10L, 20L), 99L)).isEqualTo(10L);
    }
}

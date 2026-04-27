package com.lojapp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageablesTest {

    @Test
    void clamp_capsSizeAtMaxLimit() {
        Pageable requested = PageRequest.of(0, 500, Sort.by(Sort.Direction.ASC, "name"));

        Pageable effective = Pageables.clamp(requested);

        assertThat(effective.getPageSize()).isEqualTo(Pageables.MAX_PAGE_SIZE);
    }

    @Test
    void clamp_keepsOriginalWhenWithinLimit() {
        Pageable requested = PageRequest.of(2, 40, Sort.by(Sort.Direction.DESC, "soldAt"));

        Pageable effective = Pageables.clamp(requested);

        assertThat(effective).isEqualTo(requested);
    }
}

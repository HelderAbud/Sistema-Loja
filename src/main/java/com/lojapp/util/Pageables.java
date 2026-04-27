package com.lojapp.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/** Utilitário para aplicar limites defensivos de paginação nos endpoints. */
public final class Pageables {

    public static final int MAX_PAGE_SIZE = 200;

    private Pageables() {}

    public static Pageable clamp(Pageable pageable) {
        if (!pageable.isPaged()) {
            return pageable;
        }
        int size = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        if (size != pageable.getPageSize()) {
            return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
        }
        return pageable;
    }
}

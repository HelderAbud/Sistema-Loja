package com.lojapp.dto.brand;

import com.lojapp.entity.Brand;

public record BrandResponse(Long id, String name) {
    public static BrandResponse from(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName());
    }
}

package com.lojapp.service.contract;

import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.brand.BrandResponse;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.product.ProductResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LojappCatalogServiceContract {

    List<BrandResponse> listBrands(long userId);

    BrandResponse createBrand(long userId, BrandRequest request);

    BrandResponse updateBrand(long userId, long brandId, BrandRequest request);

    void deleteBrand(long userId, long brandId);

    List<ProductResponse> listProducts(long userId);

    Page<ProductResponse> searchProducts(
            long userId, Long brandId, String q, boolean lowStock, Pageable pageable);

    ProductResponse createProduct(long userId, ProductRequest request);

    ProductResponse updateProduct(long userId, long productId, ProductRequest request);
}

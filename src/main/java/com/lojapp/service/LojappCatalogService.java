package com.lojapp.service;

import com.lojapp.entity.Brand;
import com.lojapp.entity.Product;
import com.lojapp.entity.ProductModel;
import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.brand.BrandResponse;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.product.ProductResponse;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.repository.BrandRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.exception.domain.BrandNotFoundException;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.ProductModelNotFoundException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.exception.domain.SupplierNotFoundException;
import com.lojapp.repository.ProductModelRepository;
import com.lojapp.repository.ProductSpecifications;
import com.lojapp.repository.SupplierRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.contract.LojappCatalogServiceContract;
import com.lojapp.util.EanNormalizer;
import com.lojapp.util.Pageables;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo: marcas e produtos por utilizador (isolamento multi-loja). */
@Service
public class LojappCatalogService implements LojappCatalogServiceContract {

    private final BrandRepository brands;
    private final ProductRepository products;
    private final UserRepository users;
    private final SupplierRepository suppliers;
    private final ProductModelRepository productModels;

    public LojappCatalogService(
            BrandRepository brands,
            ProductRepository products,
            UserRepository users,
            SupplierRepository suppliers,
            ProductModelRepository productModels) {
        this.brands = brands;
        this.products = products;
        this.users = users;
        this.suppliers = suppliers;
        this.productModels = productModels;
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands(long userId) {
        return brands.findByUser_IdOrderByNameAsc(userId).stream().map(BrandResponse::from).toList();
    }

    @Transactional
    public BrandResponse createBrand(long userId, BrandRequest request) {
        String name = request.name().trim();
        return brands
                .findByUser_IdAndNameIgnoreCase(userId, name)
                .map(BrandResponse::from)
                .orElseGet(
                        () -> {
                            Brand brand = new Brand();
                            brand.setUser(users.getReferenceById(userId));
                            brand.setName(name);
                            Instant now = Instant.now();
                            brand.setCreatedAt(now);
                            brand.setUpdatedAt(now);
                            return BrandResponse.from(brands.save(brand));
                        });
    }

    @Transactional
    public BrandResponse updateBrand(long userId, long brandId, BrandRequest request) {
        Brand brand =
                brands.findByIdAndUser_Id(brandId, userId).orElseThrow(BrandNotFoundException::new);
        String newName = request.name().trim();
        brands
                .findByUser_IdAndNameIgnoreCase(userId, newName)
                .filter(b -> !b.getId().equals(brand.getId()))
                .ifPresent(
                        b -> {
                            throw new LojappDomainException(
                                    ApiErrorCode.CONFLICT, "Já existe uma marca com este nome");
                        });
        brand.setName(newName);
        return BrandResponse.from(brands.save(brand));
    }

    /**
     * Remove a marca da loja. Produtos associados ficam sem marca ({@code brand_id} NULL na BD 
     * {@code ON DELETE SET NULL}).
     */
    @Transactional
    public void deleteBrand(long userId, long brandId) {
        Brand brand =
                brands.findByIdAndUser_Id(brandId, userId).orElseThrow(BrandNotFoundException::new);
        products.clearBrandForUser(brand.getId(), userId);
        brands.delete(brand);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProducts(long userId) {
        return products.findByUser_IdOrderByNameAsc(userId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    /** Listagem paginada com filtros opcionais (marca, texto no nome, apenas stock abaixo do mínimo). */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(
            long userId, Long brandId, String q, boolean lowStock, Pageable pageable) {
        Pageable effective = Pageables.clamp(pageable);
        Specification<Product> spec =
                Specification.allOf(
                        ProductSpecifications.ownedByUser(userId),
                        ProductSpecifications.brandIdEquals(brandId),
                        ProductSpecifications.nameContainsIgnoreCase(q),
                        ProductSpecifications.lowStockOnly(userId, lowStock));
        return products.findAll(spec, effective).map(ProductResponse::from);
    }

    @Transactional
    public ProductResponse createProduct(long userId, ProductRequest request) {
        Product product = new Product();
        updateProductFields(userId, product, request);
        return ProductResponse.from(products.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(long userId, long productId, ProductRequest request) {
        Product product =
                products
                        .findByIdAndUser_Id(productId, userId)
                        .orElseThrow(ProductNotFoundException::new);
        updateProductFields(userId, product, request);
        return ProductResponse.from(products.save(product));
    }

    private void updateProductFields(long userId, Product product, ProductRequest request) {
        Instant now = Instant.now();
        if (product.getId() == null) {
            product.setCreatedAt(now);
        }
        product.setUpdatedAt(now);
        product.setUser(users.getReferenceById(userId));
        product.setName(request.name().trim());
        product.setEan(EanNormalizer.forStorage(request.ean()));
        product.setNcm(request.ncm());
        product.setSku(request.sku());
        product.setCostPrice(request.costPrice());
        product.setSalePrice(request.salePrice());
        product.setMinimumStock(request.minimumStock());

        if (request.supplierId() != null) {
            product.setSupplier(
                    suppliers
                            .findByIdAndUser_Id(request.supplierId(), userId)
                            .orElseThrow(SupplierNotFoundException::new));
        } else {
            product.setSupplier(null);
        }

        if (request.productModelId() != null) {
            ProductModel model =
                    productModels
                            .findByIdAndUser_Id(request.productModelId(), userId)
                            .orElseThrow(ProductModelNotFoundException::new);
            if (request.brandId() != null
                    && !request.brandId().equals(model.getBrand().getId())) {
                throw new LojappDomainException(
                        ApiErrorCode.CONFLICT, "Marca do pedido não coincide com a marca do modelo");
            }
            product.setProductModel(model);
            product.setBrand(model.getBrand());
        } else {
            product.setProductModel(null);
            if (request.brandId() != null) {
                Brand brand =
                        brands
                                .findByIdAndUser_Id(request.brandId(), userId)
                                .orElseThrow(BrandNotFoundException::new);
                product.setBrand(brand);
            } else {
                product.setBrand(null);
            }
        }

        product.setVariantColor(blankToNull(request.variantColor()));
        product.setVariantSize(blankToNull(request.variantSize()));
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

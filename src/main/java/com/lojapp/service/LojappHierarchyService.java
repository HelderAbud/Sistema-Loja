package com.lojapp.service;

import com.lojapp.entity.Brand;
import com.lojapp.entity.ProductCollection;
import com.lojapp.entity.ProductModel;
import com.lojapp.entity.Supplier;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.hierarchy.ProductCollectionRequest;
import com.lojapp.dto.hierarchy.ProductCollectionResponse;
import com.lojapp.dto.hierarchy.ProductModelRequest;
import com.lojapp.dto.hierarchy.ProductModelResponse;
import com.lojapp.dto.supplier.SupplierRequest;
import com.lojapp.dto.supplier.SupplierResponse;
import com.lojapp.exception.domain.BrandNotFoundException;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.ProductCollectionNotFoundException;
import com.lojapp.exception.domain.ProductModelNotFoundException;
import com.lojapp.exception.domain.SupplierNotFoundException;
import com.lojapp.repository.BrandRepository;
import com.lojapp.repository.ProductCollectionRepository;
import com.lojapp.repository.ProductModelRepository;
import com.lojapp.repository.SupplierRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.contract.LojappHierarchyServiceContract;
import com.lojapp.util.TaxIdNormalizer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fornecedores, coleções e modelos (hierarquia multimarcas). */
@Service
public class LojappHierarchyService implements LojappHierarchyServiceContract {

    private final UserRepository users;
    private final BrandRepository brands;
    private final SupplierRepository suppliers;
    private final ProductCollectionRepository collections;
    private final ProductModelRepository models;

    public LojappHierarchyService(
            UserRepository users,
            BrandRepository brands,
            SupplierRepository suppliers,
            ProductCollectionRepository collections,
            ProductModelRepository models) {
        this.users = users;
        this.brands = brands;
        this.suppliers = suppliers;
        this.collections = collections;
        this.models = models;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listSuppliers(long userId) {
        return suppliers.findByUser_IdOrderByLegalNameAsc(userId).stream()
                .map(SupplierResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(long userId, long supplierId) {
        Supplier s =
                suppliers.findByIdAndUser_Id(supplierId, userId).orElseThrow(SupplierNotFoundException::new);
        return SupplierResponse.from(s);
    }

    @Transactional
    public SupplierResponse createSupplier(long userId, SupplierRequest request) {
        Optional<String> taxId = TaxIdNormalizer.forStorage(request.taxId());
        taxId.ifPresent(
                digits -> {
                    if (suppliers.existsByUser_IdAndCnpj(userId, digits)) {
                        throw new LojappDomainException(
                                ApiErrorCode.CONFLICT, "Já existe um fornecedor com este CNPJ/CPF");
                    }
                });

        Supplier s = new Supplier();
        s.setUser(users.getReferenceById(userId));
        s.setLegalName(request.legalName().trim());
        s.setCnpj(taxId.orElse(null));
        Instant now = Instant.now();
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        return SupplierResponse.from(suppliers.save(s));
    }

    /**
     * Importação NFe: reutiliza fornecedor existente pelo CNPJ/CPF ou cria um novo com o nome do emitente.
     * Sem identificação fiscal não associa (evita duplicados ambíguos só por nome).
     */
    @Transactional
    public Supplier resolveSupplierForNfeImport(
            long userId, Optional<String> supplierTaxId, String supplierName) {
        if (supplierTaxId.isEmpty()) {
            return null;
        }
        String digits = supplierTaxId.get();
        return suppliers
                .findByUser_IdAndCnpj(userId, digits)
                .orElseGet(
                        () -> {
                            Supplier s = new Supplier();
                            s.setUser(users.getReferenceById(userId));
                            s.setCnpj(digits);
                            s.setLegalName(trimEmitLegalName(supplierName));
                            Instant now = Instant.now();
                            s.setCreatedAt(now);
                            s.setUpdatedAt(now);
                            return suppliers.save(s);
                        });
    }

    private static String trimEmitLegalName(String supplierName) {
        if (supplierName == null) {
            return "Fornecedor (NFe)";
        }
        String t = supplierName.trim();
        return t.isEmpty() ? "Fornecedor (NFe)" : t;
    }

    @Transactional(readOnly = true)
    public List<ProductCollectionResponse> listCollections(long userId, long brandId) {
        ensureBrandOwned(userId, brandId);
        return collections.findByUser_IdAndBrand_IdOrderByNameAsc(userId, brandId).stream()
                .map(ProductCollectionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductCollectionResponse getCollection(long userId, long collectionId) {
        ProductCollection c =
                collections
                        .findByIdAndUser_Id(collectionId, userId)
                        .orElseThrow(ProductCollectionNotFoundException::new);
        return ProductCollectionResponse.from(c);
    }

    @Transactional
    public ProductCollectionResponse createCollection(long userId, ProductCollectionRequest request) {
        Brand brand = ensureBrandOwned(userId, request.brandId());
        String name = request.name().trim();
        collections
                .findByUser_IdAndBrand_IdAndNameIgnoreCase(userId, brand.getId(), name)
                .ifPresent(
                        c -> {
                            throw new LojappDomainException(
                                    ApiErrorCode.CONFLICT,
                                    "Já existe uma coleção com este nome nesta marca");
                        });

        ProductCollection c = new ProductCollection();
        c.setUser(users.getReferenceById(userId));
        c.setBrand(brand);
        c.setName(name);
        Instant now = Instant.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        return ProductCollectionResponse.from(collections.save(c));
    }

    @Transactional(readOnly = true)
    public List<ProductModelResponse> listModels(long userId, long brandId, Long collectionId) {
        ensureBrandOwned(userId, brandId);
        if (collectionId != null) {
            ProductCollection c =
                    collections
                            .findByIdAndUser_Id(collectionId, userId)
                            .orElseThrow(ProductCollectionNotFoundException::new);
            if (!c.getBrand().getId().equals(brandId)) {
                throw new LojappDomainException(
                        ApiErrorCode.CONFLICT, "A coleção não pertence à marca indicada");
            }
        }
        List<ProductModel> rows =
                collectionId == null
                        ? models.findByUser_IdAndBrand_IdOrderByNameAsc(userId, brandId)
                        : models.findByUser_IdAndBrand_IdAndCollection_IdOrderByNameAsc(
                                userId, brandId, collectionId);
        return rows.stream().map(ProductModelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductModelResponse getModel(long userId, long modelId) {
        ProductModel m =
                models.findByIdAndUser_Id(modelId, userId).orElseThrow(ProductModelNotFoundException::new);
        return ProductModelResponse.from(m);
    }

    @Transactional
    public ProductModelResponse createModel(long userId, ProductModelRequest request) {
        Brand brand = ensureBrandOwned(userId, request.brandId());
        String name = request.name().trim();
        models.findByUser_IdAndBrand_IdAndNameIgnoreCase(userId, brand.getId(), name)
                .ifPresent(
                        m -> {
                            throw new LojappDomainException(
                                    ApiErrorCode.CONFLICT,
                                    "Já existe um modelo com este nome nesta marca");
                        });

        ProductCollection collection = null;
        if (request.collectionId() != null) {
            collection =
                    collections
                            .findByIdAndUser_Id(request.collectionId(), userId)
                            .orElseThrow(ProductCollectionNotFoundException::new);
            if (!collection.getBrand().getId().equals(brand.getId())) {
                throw new LojappDomainException(
                        ApiErrorCode.CONFLICT, "A coleção não pertence à marca indicada");
            }
        }

        ProductModel m = new ProductModel();
        m.setUser(users.getReferenceById(userId));
        m.setBrand(brand);
        m.setCollection(collection);
        m.setName(name);
        Instant now = Instant.now();
        m.setCreatedAt(now);
        m.setUpdatedAt(now);
        return ProductModelResponse.from(models.save(m));
    }

    private Brand ensureBrandOwned(long userId, long brandId) {
        return brands.findByIdAndUser_Id(brandId, userId).orElseThrow(BrandNotFoundException::new);
    }
}

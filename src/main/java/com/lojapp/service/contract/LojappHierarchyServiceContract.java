package com.lojapp.service.contract;

import com.lojapp.dto.hierarchy.ProductCollectionRequest;
import com.lojapp.dto.hierarchy.ProductCollectionResponse;
import com.lojapp.dto.hierarchy.ProductModelRequest;
import com.lojapp.dto.hierarchy.ProductModelResponse;
import com.lojapp.dto.supplier.SupplierRequest;
import com.lojapp.dto.supplier.SupplierResponse;
import com.lojapp.entity.Supplier;
import java.util.List;
import java.util.Optional;

public interface LojappHierarchyServiceContract {

    List<SupplierResponse> listSuppliers(long userId);

    SupplierResponse getSupplier(long userId, long supplierId);

    SupplierResponse createSupplier(long userId, SupplierRequest request);

    Supplier resolveSupplierForNfeImport(long userId, Optional<String> supplierTaxId, String supplierName);

    List<ProductCollectionResponse> listCollections(long userId, long brandId);

    ProductCollectionResponse getCollection(long userId, long collectionId);

    ProductCollectionResponse createCollection(long userId, ProductCollectionRequest request);

    List<ProductModelResponse> listModels(long userId, long brandId, Long collectionId);

    ProductModelResponse getModel(long userId, long modelId);

    ProductModelResponse createModel(long userId, ProductModelRequest request);
}

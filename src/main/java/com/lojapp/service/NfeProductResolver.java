package com.lojapp.service;

import com.lojapp.entity.Brand;
import com.lojapp.entity.NfeItem;
import com.lojapp.entity.Product;
import com.lojapp.entity.Supplier;
import com.lojapp.entity.User;
import com.lojapp.repository.BrandRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.service.NfeXmlParser.ParsedNfe;
import com.lojapp.service.NfeXmlParser.ParsedNfeItem;
import com.lojapp.util.EanNormalizer;
import com.lojapp.util.NfeBrandSuggester;
import com.lojapp.util.NfeBrandSuggester.BrandCandidate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NfeProductResolver {

    public record SuggestionApplyResult(int brandAssigned, int supplierAssigned, int brandSkippedModel) {}

    /** Resultado da resolução de produto para uma linha de NFe em importação. */
    public record ProductImportResolution(Product product, boolean createdFallbackWithoutSalePrice) {}

    private final ProductRepository products;
    private final BrandRepository brands;

    public NfeProductResolver(ProductRepository products, BrandRepository brands) {
        this.products = products;
        this.brands = brands;
    }

    public Optional<BrandCandidate> suggestBrand(long userId, ParsedNfe parsed) {
        List<Brand> rows = brands.findByUser_IdOrderByNameAsc(userId);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        List<BrandCandidate> candidates =
                rows.stream().map(b -> new BrandCandidate(b.getId(), b.getName())).toList();
        List<String> descriptions =
                parsed.items().stream().map(ParsedNfeItem::description).toList();
        return NfeBrandSuggester.suggest(candidates, parsed.supplierName(), descriptions);
    }

    public Optional<Brand> resolveSuggestedBrandEntity(long userId, Optional<BrandCandidate> brandPick) {
        if (brandPick.isEmpty()) {
            return Optional.empty();
        }
        return brands.findByIdAndUser_Id(brandPick.get().id(), userId);
    }

    public ProductImportResolution resolveProductForImport(
            long userId, User user, ParsedNfeItem item, Supplier supplier) {
        Optional<String> eanOpt = resolveEanFromItem(item);
        if (eanOpt.isPresent()) {
            Optional<Product> byEan =
                    products.findFirstByUser_IdAndEan(userId, eanOpt.get());
            if (byEan.isPresent()) {
                return new ProductImportResolution(byEan.get(), false);
            }
        }
        return products
                .findByUser_IdAndNameIgnoreCase(userId, item.description())
                .map(p -> new ProductImportResolution(p, false))
                .orElseGet(
                        () ->
                                new ProductImportResolution(
                                        createFallbackProduct(user, item, supplier), true));
    }

    public SuggestionApplyResult applySuggestions(
            List<NfeItem> lines, boolean doBrand, Brand brandEntity, boolean doSupplier, Supplier entrySupplier) {
        int brandAssigned = 0;
        int supplierAssigned = 0;
        int brandSkippedModel = 0;
        List<Product> changedProducts = new ArrayList<>();

        for (NfeItem line : lines) {
            Product p = line.getProduct();
            if (p == null) {
                continue;
            }
            boolean changed = false;

            if (doBrand && brandEntity != null && p.getBrand() == null) {
                if (p.getProductModel() != null
                        && !p.getProductModel().getBrand().getId().equals(brandEntity.getId())) {
                    brandSkippedModel++;
                } else {
                    p.setBrand(brandEntity);
                    changed = true;
                    brandAssigned++;
                }
            }

            if (doSupplier && entrySupplier != null && p.getSupplier() == null) {
                p.setSupplier(entrySupplier);
                changed = true;
                supplierAssigned++;
            }

            if (changed) {
                p.setUpdatedAt(Instant.now());
                changedProducts.add(p);
            }
        }

        if (!changedProducts.isEmpty()) {
            products.saveAll(changedProducts);
        }

        return new SuggestionApplyResult(brandAssigned, supplierAssigned, brandSkippedModel);
    }

    private static Optional<String> resolveEanFromItem(ParsedNfeItem item) {
        Optional<String> fromCean = EanNormalizer.forLookup(item.cEanRaw());
        if (fromCean.isPresent()) {
            return fromCean;
        }
        return EanNormalizer.forLookup(item.cEanTribRaw());
    }

    private Product createFallbackProduct(User user, ParsedNfeItem item, Supplier supplier) {
        Product p = new Product();
        p.setUser(user);
        p.setName(item.description());
        p.setCostPrice(item.unitCost());
        p.setSalePrice(item.unitCost());
        p.setMinimumStock(BigDecimal.ZERO);
        resolveEanFromItem(item).ifPresent(p::setEan);
        if (supplier != null) {
            p.setSupplier(supplier);
        }
        return products.save(p);
    }
}

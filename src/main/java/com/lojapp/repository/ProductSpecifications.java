package com.lojapp.repository;

import com.lojapp.entity.InventoryBalance;
import com.lojapp.entity.Product;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

/** Predicados reutilizáveis para listagem filtrada de produtos (paginação). */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> ownedByUser(long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Product> brandIdEquals(Long brandId) {
        if (brandId == null) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId);
    }

    public static Specification<Product> nameContainsIgnoreCase(String q) {
        if (q == null || q.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    /**
     * Stock baixo: quantidade em saldo inferior ao mínimo, ou sem saldo registado com mínimo
     * &gt; 0.
     */
    public static Specification<Product> lowStockOnly(long userId, boolean only) {
        if (!only) {
            return (root, query, cb) -> cb.conjunction();
        }
        return (root, query, cb) -> {
            Subquery<Long> lowBalance = query.subquery(Long.class);
            Root<InventoryBalance> ibLow = lowBalance.from(InventoryBalance.class);
            lowBalance.select(ibLow.get("id"));
            lowBalance.where(
                    cb.and(
                            cb.equal(ibLow.get("user").get("id"), userId),
                            cb.equal(ibLow.get("product").get("id"), root.get("id")),
                            cb.lessThan(ibLow.get("quantity"), root.get("minimumStock"))));

            Subquery<Long> anyBalance = query.subquery(Long.class);
            Root<InventoryBalance> ibAny = anyBalance.from(InventoryBalance.class);
            anyBalance.select(ibAny.get("id"));
            anyBalance.where(
                    cb.and(
                            cb.equal(ibAny.get("user").get("id"), userId),
                            cb.equal(ibAny.get("product").get("id"), root.get("id"))));

            return cb.or(
                    cb.exists(lowBalance),
                    cb.and(
                            cb.not(cb.exists(anyBalance)),
                            cb.greaterThan(root.get("minimumStock"), BigDecimal.ZERO)));
        };
    }
}

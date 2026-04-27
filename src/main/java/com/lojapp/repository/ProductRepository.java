package com.lojapp.repository;

import com.lojapp.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    interface ProductStockRowProjection {
        Long getProductId();

        String getName();

        BigDecimal getMinimumStock();

        BigDecimal getQuantity();
    }

    interface InventoryKpiProjection {
        Long getTotalProducts();

        BigDecimal getTotalUnits();

        Long getLowStock();

        Long getWithStock();
    }

    List<Product> findByUser_IdOrderByNameAsc(Long userId);

    /**
     * Uma linha por produto do utilizador com saldo actual (0 se não existir linha em
     * {@code inventory_balances}). Evita N+1 em KPIs e stock baixo.
     */
    @Query(
            value =
                    """
                    SELECT
                      p.id AS productId,
                      p.name AS name,
                      p.minimum_stock AS minimumStock,
                      coalesce(b.quantity, 0) AS quantity
                    FROM products p
                    LEFT JOIN inventory_balances b ON b.product_id = p.id AND b.user_id = p.user_id
                    WHERE p.user_id = :userId
                    ORDER BY p.name ASC
                    """,
            nativeQuery = true)
    List<ProductStockRowProjection> findProductStockRowsForUser(@Param("userId") Long userId);

    @Query(
            value =
                    """
                    SELECT
                      p.id AS productId,
                      p.name AS name,
                      p.minimum_stock AS minimumStock,
                      coalesce(b.quantity, 0) AS quantity
                    FROM products p
                    LEFT JOIN inventory_balances b ON b.product_id = p.id AND b.user_id = p.user_id
                    WHERE p.user_id = :userId
                      AND coalesce(b.quantity, 0) < p.minimum_stock
                    ORDER BY (p.minimum_stock - coalesce(b.quantity, 0)) DESC, p.name ASC
                    """,
            nativeQuery = true)
    List<ProductStockRowProjection> findLowStockRowsForUser(@Param("userId") Long userId);

    @Query(
            value =
                    """
                    SELECT
                      COUNT(*) AS totalProducts,
                      COALESCE(SUM(COALESCE(b.quantity, 0)), 0) AS totalUnits,
                      SUM(CASE WHEN COALESCE(b.quantity, 0) < p.minimum_stock THEN 1 ELSE 0 END) AS lowStock,
                      SUM(CASE WHEN COALESCE(b.quantity, 0) > 0 THEN 1 ELSE 0 END) AS withStock
                    FROM products p
                    LEFT JOIN inventory_balances b ON b.product_id = p.id AND b.user_id = p.user_id
                    WHERE p.user_id = :userId
                    """,
            nativeQuery = true)
    InventoryKpiProjection calcInventoryKpis(@Param("userId") Long userId);

    Optional<Product> findByIdAndUser_Id(Long id, Long userId);

    Optional<Product> findByUser_IdAndNameIgnoreCase(Long userId, String name);

    Optional<Product> findFirstByUser_IdAndEan(Long userId, String ean);

    /**
     * Desassocia produtos da marca antes de apagar a linha em {@code brands}, mantendo a sessão JPA
     * consistente (alinhado a {@code ON DELETE SET NULL} na BD).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.brand = null where p.brand.id = :brandId and p.user.id = :userId")
    int clearBrandForUser(@Param("brandId") long brandId, @Param("userId") long userId);
}

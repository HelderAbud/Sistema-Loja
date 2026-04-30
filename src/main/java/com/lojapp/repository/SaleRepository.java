package com.lojapp.repository;

import com.lojapp.entity.Sale;
import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    interface BrandKpiAggregateRow {
        Long getBrandId();

        String getBrandName();

        BigDecimal getQuantity();

        BigDecimal getRevenue();

        BigDecimal getProfit();
    }

    interface ProductAbcAggregateRow {
        Long getProductId();

        String getProductName();

        String getBrandName();

        BigDecimal getQuantitySold();

        BigDecimal getRevenue();
    }

    interface SalesSummaryAggregateRow {
        BigDecimal getRevenue();

        BigDecimal getUnitsSold();

        BigDecimal getAverageTicket();
    }

    interface SalesDailyAggregateRow {
        LocalDate getSoldDate();

        BigDecimal getRevenue();

        BigDecimal getUnitsSold();
    }

    @EntityGraph(attributePaths = {"product", "product.brand"})
    List<Sale> findByUser_IdAndSoldAtBetween(Long userId, Instant from, Instant to);

    @EntityGraph(attributePaths = {"product", "product.brand"})
    Optional<Sale> findByIdAndUser_Id(long id, long userId);

    @Query(
            """
            select
                b.id as brandId,
                b.name as brandName,
                sum(s.quantity) as quantity,
                sum(s.unitPrice * s.quantity) as revenue,
                sum((s.unitPrice - s.unitCost) * s.quantity) as profit
            from Sale s
            left join s.product p
            left join p.brand b
            where s.user.id = :userId
              and s.soldAt >= :from
              and s.soldAt <= :to
              and s.cancelledAt is null
            group by b.id, b.name
            """)
    List<BrandKpiAggregateRow> aggregateBrandKpis(
            @Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(
            value =
                    """
            select
                b.id as brandId,
                b.name as brandName,
                coalesce(sum(s.quantity), 0) as quantity,
                coalesce(sum(s.unit_price * s.quantity), 0) as revenue,
                coalesce(sum((s.unit_price - s.unit_cost) * s.quantity), 0) as profit
            from sales s
            left join products p on p.id = s.product_id
            left join brands b on b.id = p.brand_id
            where s.user_id = :userId
              and s.sold_at >= :from
              and s.sold_at <= :to
              and s.cancelled_at is null
            group by b.id, b.name
            order by profit desc, revenue desc, b.id asc
            """,
            nativeQuery = true)
    List<BrandKpiAggregateRow> aggregateBrandKpisPage(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query(
            value =
                    """
            select count(*)
            from (
                select 1
                from sales s
                left join products p on p.id = s.product_id
                left join brands b on b.id = p.brand_id
                where s.user_id = :userId
                  and s.sold_at >= :from
                  and s.sold_at <= :to
                  and s.cancelled_at is null
                group by b.id, b.name
            ) grouped
            """,
            nativeQuery = true)
    long countBrandKpiGroups(
            @Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);

    @Query(
            """
            select
                p.id as productId,
                p.name as productName,
                b.name as brandName,
                sum(s.quantity) as quantitySold,
                sum(s.unitPrice * s.quantity) as revenue
            from Sale s
            join s.product p
            left join p.brand b
            where s.user.id = :userId
              and s.soldAt >= :from
              and s.soldAt <= :to
              and s.cancelledAt is null
            group by p.id, p.name, b.id, b.name
            order by sum(s.unitPrice * s.quantity) desc, p.id asc
            """)
    List<ProductAbcAggregateRow> aggregateProductAbc(
            @Param("userId") Long userId, @Param("from") Instant from, @Param("to") Instant to);

    @EntityGraph(attributePaths = {"product", "product.brand"})
    @Query(
            """
            select s from Sale s
            where s.user.id = :userId
              and s.soldAt >= :from
              and s.soldAt <= :to
              and (:productId is null or s.product.id = :productId)
              and (:brandId is null or s.product.brand.id = :brandId)
            """)
    Page<Sale> searchForUser(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("productId") Long productId,
            @Param("brandId") Long brandId,
            Pageable pageable);

    default Page<Sale> searchForUser(
            Long userId, Instant from, Instant to, Long productId, Pageable pageable) {
        return searchForUser(userId, from, to, productId, null, pageable);
    }

    @Query(
            """
            select
                coalesce(sum(s.unitPrice * s.quantity), 0) as revenue,
                coalesce(sum(s.quantity), 0) as unitsSold,
                coalesce(avg(s.unitPrice * s.quantity), 0) as averageTicket
            from Sale s
            where s.user.id = :userId
              and s.soldAt >= :from
              and s.soldAt <= :to
              and (:productId is null or s.product.id = :productId)
              and (:brandId is null or s.product.brand.id = :brandId)
              and s.cancelledAt is null
            """)
    SalesSummaryAggregateRow aggregateSalesSummary(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("productId") Long productId,
            @Param("brandId") Long brandId);

    @Query(
            value =
                    """
            select
                date(s.sold_at) as soldDate,
                coalesce(sum(s.unit_price * s.quantity), 0) as revenue,
                coalesce(sum(s.quantity), 0) as unitsSold
            from sales s
            join products p on p.id = s.product_id
            where s.user_id = :userId
              and s.sold_at >= :from
              and s.sold_at <= :to
              and s.cancelled_at is null
              and (:productId is null or s.product_id = :productId)
              and (:brandId is null or p.brand_id = :brandId)
            group by date(s.sold_at)
            order by date(s.sold_at) asc
            """,
            nativeQuery = true)
    List<SalesDailyAggregateRow> aggregateSalesDaily(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("productId") Long productId,
            @Param("brandId") Long brandId);
}

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
                sum(i.quantity) as quantity,
                sum(i.unitPrice * i.quantity) as revenue,
                sum((i.unitPrice - i.unitCost) * i.quantity) as profit
            from SaleItem i
            join i.sale s
            join i.product p
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
                coalesce(sum(i.quantity), 0) as quantity,
                coalesce(sum(i.unit_price * i.quantity), 0) as revenue,
                coalesce(sum((i.unit_price - i.unit_cost) * i.quantity), 0) as profit
            from sale_items i
            join sales s on s.id = i.sale_id
            join products p on p.id = i.product_id
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
                from sale_items i
                join sales s on s.id = i.sale_id
                join products p on p.id = i.product_id
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
                sum(i.quantity) as quantitySold,
                sum(i.unitPrice * i.quantity) as revenue
            from SaleItem i
            join i.sale s
            join i.product p
            left join p.brand b
            where s.user.id = :userId
              and s.soldAt >= :from
              and s.soldAt <= :to
              and s.cancelledAt is null
            group by p.id, p.name, b.id, b.name
            order by sum(i.unitPrice * i.quantity) desc, p.id asc
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
              and (:productId is null or exists (
                    select 1 from SaleItem i
                    where i.sale = s and i.product.id = :productId))
              and (:brandId is null or exists (
                    select 1 from SaleItem i join i.product p
                    where i.sale = s and p.brand.id = :brandId))
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
            value =
                    """
            select
                coalesce(sum(ticket.revenue), 0) as revenue,
                coalesce(sum(ticket.units), 0) as unitsSold,
                coalesce(avg(ticket.revenue), 0) as averageTicket
            from (
                select
                    s.id as sale_id,
                    sum(i.unit_price * i.quantity) as revenue,
                    sum(i.quantity) as units
                from sales s
                join sale_items i on i.sale_id = s.id
                join products p on p.id = i.product_id
                where s.user_id = :userId
                  and s.sold_at >= :from
                  and s.sold_at <= :to
                  and s.cancelled_at is null
                  and (:productId is null or i.product_id = :productId)
                  and (:brandId is null or p.brand_id = :brandId)
                group by s.id
            ) ticket
            """,
            nativeQuery = true)
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
                coalesce(sum(i.unit_price * i.quantity), 0) as revenue,
                coalesce(sum(i.quantity), 0) as unitsSold
            from sales s
            join sale_items i on i.sale_id = s.id
            join products p on p.id = i.product_id
            where s.user_id = :userId
              and s.sold_at >= :from
              and s.sold_at <= :to
              and s.cancelled_at is null
              and (:productId is null or i.product_id = :productId)
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

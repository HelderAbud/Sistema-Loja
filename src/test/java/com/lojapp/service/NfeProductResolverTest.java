package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import com.lojapp.repository.BrandRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.service.NfeXmlParser.ParsedNfeItem;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NfeProductResolverTest {

    @Mock private ProductRepository products;
    @Mock private BrandRepository brands;

    private NfeProductResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new NfeProductResolver(products, brands);
    }

    @Test
    void matchByEan_updatesCostToLastNfeUnitCost() {
        User user = new User();
        Product existing = new Product();
        existing.setId(9L);
        existing.setCostPrice(new BigDecimal("10.00"));
        existing.setSalePrice(new BigDecimal("30.00"));
        existing.setEan("7891234567890");

        when(products.findFirstByUser_IdAndEanAndDeletedAtIsNull(1L, "7891234567890"))
                .thenReturn(Optional.of(existing));
        when(products.save(existing)).thenReturn(existing);

        ParsedNfeItem item =
                new ParsedNfeItem(
                        "Nome XML",
                        "7891234567890",
                        null,
                        BigDecimal.ONE,
                        new BigDecimal("20.00"));

        var resolution = resolver.resolveProductForImport(1L, user, item, null);

        assertThat(resolution.product().getCostPrice()).isEqualByComparingTo("20.00");
        assertThat(resolution.product().getSalePrice()).isEqualByComparingTo("30.00");
        assertThat(resolution.createdFallbackWithoutSalePrice()).isFalse();
        verify(products).save(existing);
    }

    @Test
    void matchByEan_sameCost_doesNotSave() {
        User user = new User();
        Product existing = new Product();
        existing.setCostPrice(new BigDecimal("20.00"));
        existing.setSalePrice(new BigDecimal("30.00"));
        existing.setEan("7891234567890");

        when(products.findFirstByUser_IdAndEanAndDeletedAtIsNull(1L, "7891234567890"))
                .thenReturn(Optional.of(existing));

        ParsedNfeItem item =
                new ParsedNfeItem(
                        "Nome XML",
                        "7891234567890",
                        null,
                        BigDecimal.ONE,
                        new BigDecimal("20.00"));

        resolver.resolveProductForImport(1L, user, item, null);

        verify(products, never()).save(any());
    }
}

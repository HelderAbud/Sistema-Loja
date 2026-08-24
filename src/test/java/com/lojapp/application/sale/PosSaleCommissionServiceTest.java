package com.lojapp.application.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lojapp.entity.Brand;
import com.lojapp.entity.CashSession;
import com.lojapp.entity.CommissionAccrual;
import com.lojapp.entity.CommissionRule;
import com.lojapp.entity.Product;
import com.lojapp.entity.Sale;
import com.lojapp.entity.SaleItem;
import com.lojapp.entity.Seller;
import com.lojapp.entity.SellerQueueState;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.SellerInactiveException;
import com.lojapp.repository.CommissionAccrualRepository;
import com.lojapp.repository.CommissionRuleRepository;
import com.lojapp.repository.SellerQueueStateRepository;
import com.lojapp.repository.SellerRepository;
import com.lojapp.service.AuditService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PosSaleCommissionServiceTest {

    @Mock private SellerRepository sellers;
    @Mock private SellerQueueStateRepository queueStates;
    @Mock private CommissionRuleRepository rules;
    @Mock private CommissionAccrualRepository accruals;
    @Mock private AuditService auditService;

    private PosSaleCommissionService service;
    private User owner;
    private CashSession session;
    private Sale sale;

    @BeforeEach
    void setUp() {
        service =
                new PosSaleCommissionService(sellers, queueStates, rules, accruals, auditService);
        owner = new User();
        owner.setId(1L);
        session = new CashSession();
        session.setId(7L);
        sale = new Sale();
        sale.setId(88L);
        sale.setSoldAt(Instant.parse("2026-06-01T12:00:00Z"));
    }

    @Test
    void assign_whenNoActiveSellers_doesNothing() {
        when(sellers.findByUser_IdAndActiveTrueOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of());

        service.assignSellerAndAccrue(1L, owner, session, sale, List.of(), null);

        assertThat(sale.getSeller()).isNull();
        verify(accruals, never()).save(any());
    }

    @Test
    void assign_roundRobin_twoSalesAlternate() {
        Seller ana = seller(11L, "Ana");
        Seller bia = seller(12L, "Bia");
        when(sellers.findByUser_IdAndActiveTrueOrderBySortOrderAscIdAsc(1L))
                .thenReturn(List.of(ana, bia));
        when(queueStates.findById(7L)).thenReturn(Optional.empty());
        when(rules.findByUser_Id(1L)).thenReturn(List.of());

        service.assignSellerAndAccrue(1L, owner, session, sale, List.of(), null);
        assertThat(sale.getSeller().getId()).isEqualTo(11L);

        SellerQueueState state = new SellerQueueState();
        state.setCashSessionId(7L);
        state.setLastAssignedSeller(ana);
        when(queueStates.findById(7L)).thenReturn(Optional.of(state));

        Sale sale2 = new Sale();
        sale2.setId(89L);
        sale2.setSoldAt(sale.getSoldAt());
        service.assignSellerAndAccrue(1L, owner, session, sale2, List.of(), null);
        assertThat(sale2.getSeller().getId()).isEqualTo(12L);
    }

    @Test
    void assign_manualInactive_throws() {
        Seller ana = seller(11L, "Ana");
        ana.setActive(false);
        when(sellers.findByIdAndUser_Id(11L, 1L)).thenReturn(Optional.of(ana));

        assertThatThrownBy(
                        () ->
                                service.assignSellerAndAccrue(
                                        1L, owner, session, sale, List.of(), 11L))
                .isInstanceOf(SellerInactiveException.class);
        verify(auditService, never()).log(any(), any(), any());
    }

    @Test
    void accrue_brandRule_beatsGlobal() {
        Seller ana = seller(11L, "Ana");
        when(sellers.findByUser_IdAndActiveTrueOrderBySortOrderAscIdAsc(1L)).thenReturn(List.of(ana));
        when(queueStates.findById(7L)).thenReturn(Optional.empty());

        Brand brand = new Brand();
        brand.setId(8L);
        Product product = new Product();
        product.setBrand(brand);
        SaleItem item = new SaleItem();
        item.setProduct(product);
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("10.00"));

        CommissionRule global = rule(1L, null, "5.0000");
        CommissionRule byBrand = rule(2L, brand, "12.0000");
        when(rules.findByUser_Id(1L)).thenReturn(List.of(global, byBrand));
        when(rules.getReferenceById(2L)).thenReturn(byBrand);

        service.assignSellerAndAccrue(1L, owner, session, sale, List.of(item), null);

        ArgumentCaptor<CommissionAccrual> captor = ArgumentCaptor.forClass(CommissionAccrual.class);
        verify(accruals).save(captor.capture());
        assertThat(captor.getValue().getPercent()).isEqualByComparingTo("12.0000");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("2.40");
        assertThat(captor.getValue().getBaseAmount()).isEqualByComparingTo("20.00");
        assertThat(sale.getSeller()).isEqualTo(ana);
    }

    private static Seller seller(long id, String name) {
        Seller s = new Seller();
        s.setId(id);
        s.setDisplayName(name);
        s.setActive(true);
        return s;
    }

    private static CommissionRule rule(long id, Brand brand, String percent) {
        CommissionRule r = new CommissionRule();
        r.setId(id);
        r.setBrand(brand);
        r.setPercent(new BigDecimal(percent));
        r.setValidFrom(Instant.parse("2026-01-01T00:00:00Z"));
        return r;
    }
}

package com.lojapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.entity.User;
import com.lojapp.dto.brand.BrandRequest;
import com.lojapp.dto.dashboard.BrandDashboardResponse;
import com.lojapp.dto.dashboard.BrandKpiResponse;
import com.lojapp.dto.dashboard.InventoryKpiResponse;
import com.lojapp.dto.dashboard.ProductAbcResponse;
import com.lojapp.dto.inventory.StockAdjustmentRequest;
import com.lojapp.dto.nfe.NfeApplySuggestionsRequest;
import com.lojapp.dto.nfe.NfeImportResponse;
import com.lojapp.dto.ApiErrorCode;
import com.lojapp.dto.product.ProductRequest;
import com.lojapp.dto.product.ProductResponse;
import com.lojapp.dto.sale.SaleRequest;
import com.lojapp.entity.Sale;
import com.lojapp.exception.domain.BrandNotFoundException;
import com.lojapp.exception.domain.DuplicateNfeAccessKeyException;
import com.lojapp.exception.domain.DuplicateNfeXmlContentException;
import com.lojapp.exception.domain.InsufficientStockException;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.exception.domain.ProductNotFoundException;
import com.lojapp.repository.InventoryBalanceRepository;
import com.lojapp.repository.InventoryMovementRepository;
import com.lojapp.repository.NfeEntryRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SaleRepository;
import com.lojapp.application.nfe.ApplyNfeImportSuggestionsUseCase;
import com.lojapp.application.nfe.ImportNfeUseCase;
import com.lojapp.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integraÃ§Ã£o leves do nÃºcleo da loja (serviÃ§o + JPA + H2 em memÃ³ria).
 *
 * <p>PorquÃª: o guia {@code 10-guia-junior-piloto-deploy-proximos-passos.md} (Parte 2) alinha testes aos
 * fluxos antes de refatorar NFe, dashboard, etc. Cada mÃ©todo {@code @Test} cobre um fluxo pequeno e legÃ­vel. Os serviÃ§os expostos
 * pelos controllers em {@code com.lojapp.controller} sÃ£o injetados aqui em separado (sem fachada).
 *
 * <p>{@code @Transactional} na classe faz rollback ao fim de cada teste â€” a base fica limpa sem
 * apagar dados Ã  mÃ£o.
 */
@SpringBootTest
@Transactional
class LojappCoreServiceTest {

    @Autowired private LojappCatalogService catalog;
    @Autowired private ImportNfeUseCase importNfeUseCase;
    @Autowired private ApplyNfeImportSuggestionsUseCase applyNfeImportSuggestionsUseCase;
    @Autowired private InventoryService inventory;
    @Autowired private SalesService sales;
    @Autowired private DashboardService dashboard;
    @Autowired private UserRepository userRepository;
    @Autowired private InventoryBalanceRepository inventoryBalanceRepository;
    @Autowired private InventoryMovementRepository inventoryMovementRepository;
    @Autowired private NfeEntryRepository nfeEntryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SaleRepository saleRepository;

    private long userId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("loja-teste-" + Instant.now().toEpochMilli() + "@test.local");
        user.setPasswordHash("noop");
        user.setAppRole("LOJA_USER");
        userId = userRepository.save(user).getId();
    }

    @Test
    void adjustStock_increasesBalance() {
        ProductResponse product = criarProdutoSimples("Caderno", "5.00", "8.00", "0");

        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "INVENTARIO"));

        assertThat(saldo(product.id())).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(inventoryMovementRepository.findAll()).hasSize(1);
    }

    @Test
    void listLowStock_includesProductWhenBelowMinimum() {
        ProductResponse product = criarProdutoComMinimo("Caneta", new BigDecimal("5"));

        var low = inventory.listLowStock(userId);
        assertThat(low).hasSize(1);
        assertThat(low.getFirst().productId()).isEqualTo(product.id());
        assertThat(low.getFirst().currentQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registerSale_decreasesBalance() {
        ProductResponse product = criarProdutoSimples("Borracha", "1.00", "2.00", "0");
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("10"), "ENTRADA"));

        long saleId =
                sales.registerSale(
                        userId,
                        new SaleRequest(
                                product.id(),
                                new BigDecimal("3"),
                                new BigDecimal("2.00"),
                                new BigDecimal("1.00")))
                        .id();

        assertThat(saleId).isPositive();
        assertThat(saldo(product.id())).isEqualByComparingTo(new BigDecimal("7"));
    }

    @Test
    void registerSale_insufficientStock_returns400() {
        ProductResponse product = criarProdutoSimples("StockLimite", "1.00", "2.00", "0");
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("2"), "ENTRADA"));

        assertThatThrownBy(
                        () ->
        sales.registerSale(
                                        userId,
                                        new SaleRequest(
                                                product.id(),
                                                new BigDecimal("3"),
                                                new BigDecimal("2.00"),
                                                new BigDecimal("1.00"))))
                .isInstanceOf(InsufficientStockException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void registerSale_nullUnitCost_usesProductCostPrice() {
        ProductResponse product = criarProdutoSimples("CustoPadrao", "7.50", "12.00", "0");
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("5"), "ENTRADA"));

        long saleId =
                sales.registerSale(
                        userId,
                        new SaleRequest(product.id(), BigDecimal.ONE, new BigDecimal("20.00"), null))
                        .id();

        Sale sale = saleRepository.findById(saleId).orElseThrow();
        assertThat(sale.getUnitCost()).isEqualByComparingTo(new BigDecimal("7.50"));
    }

    @Test
    void brandDashboard_singleBrand_computesFaturamentoAndLucro() {
        var marca = catalog.createBrand(userId, new BrandRequest("Marca X"));
        ProductResponse product =
catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Item",
                                marca.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("4.00"),
                                new BigDecimal("10.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("100"), "ENTRADA"));

        sales.registerSale(
                userId,
                new SaleRequest(
                        product.id(),
                        new BigDecimal("2"),
                        new BigDecimal("10.00"),
                        new BigDecimal("4.00")));

        // Janela definida depois da venda: evita falhar se sold_at for alguns ms depois de um Instant capturado antes.
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.MINUTES);
        BrandDashboardResponse dash = dashboard.brandDashboard(userId, from, to, 50, 0);
        assertThat(dash.metrics()).hasSize(1);
        BrandKpiResponse kpi = dash.metrics().getFirst();
        assertThat(kpi.brand()).isEqualTo("Marca X");
        assertThat(kpi.faturamento()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(kpi.lucro()).isEqualByComparingTo(new BigDecimal("12.00"));
    }

    /**
     * Maior faturamento nÃ£o implica maior lucro: a primeira linha do dashboard deve ser a marca com
     * maior lucro no perÃ­odo (critÃ©rio MVP).
     */
    @Test
    void brandDashboard_ordersByLucroDesc_lucroWinsOverFaturamento() {
        var marcaAltoFaturamento = catalog.createBrand(userId, new BrandRequest("Marca Alto Fat"));
        var marcaMaisLucro = catalog.createBrand(userId, new BrandRequest("Marca Mais Lucro"));

        ProductResponse prodA =
catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod A",
                                marcaAltoFaturamento.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("9.00"),
                                new BigDecimal("10.00"),
                                BigDecimal.ZERO));
        ProductResponse prodB =
catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod B",
                                marcaMaisLucro.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("50.00"),
                                BigDecimal.ZERO));

        inventory.adjustStock(
                userId, new StockAdjustmentRequest(prodA.id(), new BigDecimal("1000"), "ENTRADA"));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(prodB.id(), new BigDecimal("100"), "ENTRADA"));

        // A: 100 Ã— 10 = 1000 faturamento; lucro = 100 Ã— (10 - 9) = 100
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodA.id(),
                        new BigDecimal("100"),
                        new BigDecimal("10.00"),
                        new BigDecimal("9.00")));
        // B: 10 Ã— 50 = 500 faturamento; lucro = 10 Ã— (50 - 10) = 400
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodB.id(),
                        new BigDecimal("10"),
                        new BigDecimal("50.00"),
                        new BigDecimal("10.00")));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.MINUTES);
        BrandDashboardResponse dash = dashboard.brandDashboard(userId, from, to, 50, 0);

        assertThat(dash.metrics()).hasSize(2);
        assertThat(dash.metrics().getFirst().brand()).isEqualTo("Marca Mais Lucro");
        assertThat(dash.metrics().getFirst().lucro()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(dash.metrics().get(1).brand()).isEqualTo("Marca Alto Fat");
        assertThat(dash.metrics().get(1).faturamento()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void brandDashboard_paginatesWithOffsetInsidePage_windowIsStable() {
        var brandA = catalog.createBrand(userId, new BrandRequest("Marca A"));
        var brandB = catalog.createBrand(userId, new BrandRequest("Marca B"));
        var brandC = catalog.createBrand(userId, new BrandRequest("Marca C"));

        ProductResponse prodA =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod A",
                                brandA.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("5.00"),
                                new BigDecimal("20.00"),
                                BigDecimal.ZERO));
        ProductResponse prodB =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod B",
                                brandB.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("30.00"),
                                BigDecimal.ZERO));
        ProductResponse prodC =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod C",
                                brandC.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("12.00"),
                                new BigDecimal("22.00"),
                                BigDecimal.ZERO));

        inventory.adjustStock(userId, new StockAdjustmentRequest(prodA.id(), new BigDecimal("100"), "ENTRADA"));
        inventory.adjustStock(userId, new StockAdjustmentRequest(prodB.id(), new BigDecimal("100"), "ENTRADA"));
        inventory.adjustStock(userId, new StockAdjustmentRequest(prodC.id(), new BigDecimal("100"), "ENTRADA"));

        // Lucros: A=1500, B=1000, C=500 (ordem esperada A, B, C).
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodA.id(), new BigDecimal("100"), new BigDecimal("20.00"), new BigDecimal("5.00")));
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodB.id(), new BigDecimal("50"), new BigDecimal("30.00"), new BigDecimal("10.00")));
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodC.id(), new BigDecimal("50"), new BigDecimal("22.00"), new BigDecimal("12.00")));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.MINUTES);

        BrandDashboardResponse dash = dashboard.brandDashboard(userId, from, to, 2, 1);

        assertThat(dash.totalBrands()).isEqualTo(3);
        assertThat(dash.brandLimit()).isEqualTo(2);
        assertThat(dash.brandOffset()).isEqualTo(1);
        assertThat(dash.metrics()).extracting(BrandKpiResponse::brand).containsExactly("Marca B", "Marca C");
    }

    @Test
    void brandDashboard_offsetBeyondTotal_returnsEmptyMetricsAndKeepsTotal() {
        var brandA = catalog.createBrand(userId, new BrandRequest("Marca O1"));
        var brandB = catalog.createBrand(userId, new BrandRequest("Marca O2"));

        ProductResponse prodA =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod O1",
                                brandA.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("5.00"),
                                new BigDecimal("10.00"),
                                BigDecimal.ZERO));
        ProductResponse prodB =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod O2",
                                brandB.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("4.00"),
                                new BigDecimal("12.00"),
                                BigDecimal.ZERO));

        inventory.adjustStock(userId, new StockAdjustmentRequest(prodA.id(), new BigDecimal("20"), "ENTRADA"));
        inventory.adjustStock(userId, new StockAdjustmentRequest(prodB.id(), new BigDecimal("20"), "ENTRADA"));
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodA.id(), new BigDecimal("2"), new BigDecimal("10.00"), new BigDecimal("5.00")));
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodB.id(), new BigDecimal("2"), new BigDecimal("12.00"), new BigDecimal("4.00")));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.MINUTES);

        BrandDashboardResponse dash = dashboard.brandDashboard(userId, from, to, 2, 10);

        assertThat(dash.totalBrands()).isEqualTo(2);
        assertThat(dash.brandLimit()).isEqualTo(2);
        assertThat(dash.brandOffset()).isEqualTo(10);
        assertThat(dash.metrics()).isEmpty();
    }

    @Test
    void brandDashboard_secondPageReturnsRemainder_whenTotalNotMultipleOfLimit() {
        var brandA = catalog.createBrand(userId, new BrandRequest("Marca P1"));
        var brandB = catalog.createBrand(userId, new BrandRequest("Marca P2"));
        var brandC = catalog.createBrand(userId, new BrandRequest("Marca P3"));

        ProductResponse prodA =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod P1",
                                brandA.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("5.00"),
                                new BigDecimal("25.00"),
                                BigDecimal.ZERO));
        ProductResponse prodB =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod P2",
                                brandB.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("10.00"),
                                new BigDecimal("30.00"),
                                BigDecimal.ZERO));
        ProductResponse prodC =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod P3",
                                brandC.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("12.00"),
                                new BigDecimal("22.00"),
                                BigDecimal.ZERO));

        inventory.adjustStock(userId, new StockAdjustmentRequest(prodA.id(), new BigDecimal("50"), "ENTRADA"));
        inventory.adjustStock(userId, new StockAdjustmentRequest(prodB.id(), new BigDecimal("50"), "ENTRADA"));
        inventory.adjustStock(userId, new StockAdjustmentRequest(prodC.id(), new BigDecimal("50"), "ENTRADA"));

        // Lucro esperado: P1=1000, P2=500, P3=300 -> ordem P1, P2, P3.
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodA.id(), new BigDecimal("50"), new BigDecimal("25.00"), new BigDecimal("5.00")));
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodB.id(), new BigDecimal("25"), new BigDecimal("30.00"), new BigDecimal("10.00")));
        sales.registerSale(
                userId,
                new SaleRequest(
                        prodC.id(), new BigDecimal("30"), new BigDecimal("22.00"), new BigDecimal("12.00")));

        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.MINUTES);

        BrandDashboardResponse dash = dashboard.brandDashboard(userId, from, to, 2, 2);

        assertThat(dash.totalBrands()).isEqualTo(3);
        assertThat(dash.metrics()).hasSize(1);
        assertThat(dash.metrics().getFirst().brand()).isEqualTo("Marca P3");
    }

    @Test
    void productAbc_singleProduct_classA() {
        var marca = catalog.createBrand(userId, new BrandRequest("Marca ABC"));
        ProductResponse product =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "SKU ABC",
                                marca.id(),
                                null,
                                null,
                                null,
                                new BigDecimal("4.00"),
                                new BigDecimal("10.00"),
                                BigDecimal.ZERO));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("50"), "ENTRADA"));
        sales.registerSale(
                userId,
                new SaleRequest(
                        product.id(),
                        new BigDecimal("2"),
                        new BigDecimal("10.00"),
                        new BigDecimal("4.00")));
        Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant to = Instant.now().plus(1, ChronoUnit.MINUTES);
        ProductAbcResponse abc = dashboard.productAbc(userId, from, to);
        assertThat(abc.rows()).hasSize(1);
        assertThat(abc.rows().getFirst().abcClass()).isEqualTo("A");
        assertThat(abc.totalRevenue()).isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void dashboard_rejectsRangeWiderThanMaxDays() {
        Instant from = Instant.now().minus(400, ChronoUnit.DAYS);
        Instant to = Instant.now();
        assertThatThrownBy(() -> dashboard.brandDashboard(userId, from, to, 50, 0))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void inventoryKpis_reflectsStock() {
        ProductResponse product = criarProdutoSimples("KPI Item", "1.00", "2.00", "5");
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("3"), "ENTRADA"));
        InventoryKpiResponse k = inventory.inventoryKpis(userId);
        assertThat(k.totalSkus()).isGreaterThanOrEqualTo(1);
        assertThat(k.totalUnits()).isGreaterThanOrEqualTo(new BigDecimal("3"));
        assertThat(k.lowStockCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void importNfe_persistsEntry_movementsAndBalance() {
        String xml =
                """
                <nfe>
                  <nNF>1001</nNF>
                  <xNome>Fornecedor Integracao</xNome>
                  <chNFe>35200199999999999999550010000010011000000000</chNFe>
                  <prod>
                    <xProd>Produto Via XML</xProd>
                    <qCom>2</qCom>
                    <vUnCom>15.00</vUnCom>
                  </prod>
                </nfe>
                """;

        NfeImportResponse response = importNfeUseCase.execute(userId, xml);

        assertThat(response.nfeNumber()).isEqualTo("1001");
        assertThat(response.importedItems()).isEqualTo(1);
        assertThat(response.supplierId()).isNull();
        assertThat(response.suggestedBrandId()).isNull();
        assertThat(response.suggestedBrandName()).isNull();
        assertThat(nfeEntryRepository.findAll()).hasSize(1);

        List<ProductResponse> products = catalog.listProducts(userId);
        assertThat(products.stream().map(ProductResponse::name).anyMatch(n -> n.equals("Produto Via XML")))
                .isTrue();
        ProductResponse imported =
                products.stream().filter(p -> p.name().equals("Produto Via XML")).findFirst().orElseThrow();
        assertThat(saldo(imported.id())).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(imported.costPrice()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void importNfe_emitCnpj_createsSupplierAndLinksEntry() {
        String xml =
                """
                <nfe>
                  <nNF>7007</nNF>
                  <emit>
                    <CNPJ>11222333000181</CNPJ>
                    <xNome>Fornecedor Emit CNPJ</xNome>
                  </emit>
                  <chNFe>35200777777777777777550070000070077000000000</chNFe>
                  <prod>
                    <xProd>Item Com Emitente</xProd>
                    <qCom>1</qCom>
                    <vUnCom>5.00</vUnCom>
                  </prod>
                </nfe>
                """;

        NfeImportResponse r = importNfeUseCase.execute(userId, xml);

        assertThat(r.supplierId()).isNotNull();
        var entry =
                nfeEntryRepository.findById(r.nfeEntryId()).orElseThrow();
        assertThat(entry.getSupplier()).isNotNull();
        assertThat(entry.getSupplier().getId()).isEqualTo(r.supplierId());
        assertThat(entry.getSupplier().getCnpj()).isEqualTo("11222333000181");
        assertThat(entry.getSupplier().getLegalName()).isEqualTo("Fornecedor Emit CNPJ");
    }

    @Test
    void importNfe_fallbackProduct_linksSupplierWhenEmitHasCnpj() {
        String xml =
                """
                <nfe>
                  <nNF>8008</nNF>
                  <emit>
                    <CNPJ>99888777000155</CNPJ>
                    <xNome>Distrib Novo SKU</xNome>
                  </emit>
                  <chNFe>35200888888888888888550080000080088000000000</chNFe>
                  <prod>
                    <xProd>Produto So NFe Novo</xProd>
                    <qCom>1</qCom>
                    <vUnCom>3.00</vUnCom>
                  </prod>
                </nfe>
                """;

        importNfeUseCase.execute(userId, xml);

        var p =
                productRepository
                        .findByUser_IdAndNameIgnoreCase(userId, "Produto So NFe Novo")
                        .orElseThrow();
        assertThat(p.getSupplier()).isNotNull();
        assertThat(p.getSupplier().getCnpj()).isEqualTo("99888777000155");
    }

    @Test
    void importNfe_suggestsBrandWhenNameAppearsInItemText() {
        var marca = catalog.createBrand(userId, new BrandRequest("Rainha"));
        String xml =
                """
                <nfe>
                  <nNF>8010</nNF>
                  <xNome>Fornecedor Gen</xNome>
                  <chNFe>35200999999999999999550080100080109000000000</chNFe>
                  <prod>
                    <xProd>Meia Rainha canelada</xProd>
                    <qCom>1</qCom>
                    <vUnCom>2.00</vUnCom>
                  </prod>
                </nfe>
                """;

        NfeImportResponse r = importNfeUseCase.execute(userId, xml);

        assertThat(r.suggestedBrandId()).isEqualTo(marca.id());
        assertThat(r.suggestedBrandName()).isEqualTo("Rainha");
    }

    @Test
    void applyImportSuggestions_assignsBrandToNewProductWithoutBrand() {
        catalog.createBrand(userId, new BrandRequest("Rainha"));
        String xml =
                """
                <nfe>
                  <nNF>9033</nNF>
                  <emit>
                    <CNPJ>44556677000199</CNPJ>
                    <xNome>Distrib Apply Brand</xNome>
                  </emit>
                  <chNFe>35200888888888888888550090300090333000000000</chNFe>
                  <prod>
                    <xProd>Meia Rainha apply SKU Ãºnico</xProd>
                    <qCom>1</qCom>
                    <vUnCom>2.00</vUnCom>
                  </prod>
                </nfe>
                """;

        NfeImportResponse imp = importNfeUseCase.execute(userId, xml);
        var product =
                productRepository
                        .findByUser_IdAndNameIgnoreCase(userId, "Meia Rainha apply SKU Ãºnico")
                        .orElseThrow();
        assertThat(product.getBrand()).isNull();
        assertThat(product.getSupplier()).isNotNull();

        var applied = applyNfeImportSuggestionsUseCase.execute(userId, imp.nfeEntryId(), null);
        assertThat(applied.brandAssignedCount()).isEqualTo(1);
        assertThat(applied.supplierAssignedCount()).isZero();

        product = productRepository.findById(product.getId()).orElseThrow();
        assertThat(product.getBrand()).isNotNull();
        assertThat(product.getBrand().getName()).isEqualTo("Rainha");
    }

    @Test
    void applyImportSuggestions_assignsSupplierToEanMatchedProductWithoutSupplier() {
        catalog.createProduct(
                userId,
                new ProductRequest(
                        "Prod EAN sem fornecedor",
                        null,
                        "7890001112223",
                        null,
                        null,
                        new BigDecimal("1.00"),
                        new BigDecimal("2.00"),
                        BigDecimal.ZERO));

        String xml =
                """
                <nfe>
                  <nNF>9044</nNF>
                  <emit>
                    <CNPJ>33445566000177</CNPJ>
                    <xNome>Emit p/ apply supplier</xNome>
                  </emit>
                  <chNFe>35200777777777777777550090400090444000000000</chNFe>
                  <prod>
                    <xProd>Qualquer desc apply sup</xProd>
                    <cEAN>7890001112223</cEAN>
                    <qCom>1</qCom>
                    <vUnCom>5.00</vUnCom>
                  </prod>
                </nfe>
                """;

        NfeImportResponse imp = importNfeUseCase.execute(userId, xml);
        var product =
                productRepository
                        .findFirstByUser_IdAndEan(userId, "7890001112223")
                        .orElseThrow();
        assertThat(product.getSupplier()).isNull();

        var applied = applyNfeImportSuggestionsUseCase.execute(userId, imp.nfeEntryId(), new NfeApplySuggestionsRequest(false, true));
        assertThat(applied.supplierAssignedCount()).isEqualTo(1);
        assertThat(applied.brandAssignedCount()).isZero();

        product = productRepository.findById(product.getId()).orElseThrow();
        assertThat(product.getSupplier()).isNotNull();
        assertThat(product.getSupplier().getCnpj()).isEqualTo("33445566000177");
    }

    @Test
    void importNfe_matchExistingByEan_doesNotAttachSupplierToCatalogProduct() {
        catalog.createProduct(
                userId,
                new ProductRequest(
                        "Prod Existente EAN",
                        null,
                        "7891112223334",
                        null,
                        null,
                        new BigDecimal("1.00"),
                        new BigDecimal("2.00"),
                        BigDecimal.ZERO));

        String xml =
                """
                <nfe>
                  <nNF>8011</nNF>
                  <emit><CNPJ>88776655000144</CNPJ><xNome>Emit EAN</xNome></emit>
                  <chNFe>35200111111111111111500801100080111000000000</chNFe>
                  <prod>
                    <xProd>Qualquer desc</xProd>
                    <cEAN>7891112223334</cEAN>
                    <qCom>1</qCom>
                    <vUnCom>5.00</vUnCom>
                  </prod>
                </nfe>
                """;

        importNfeUseCase.execute(userId, xml);

        var p =
                productRepository
                        .findFirstByUser_IdAndEan(userId, "7891112223334")
                        .orElseThrow();
        assertThat(p.getSupplier()).isNull();
    }

    @Test
    void importNfe_matchesByCeanTribWhenCeanIsSemGtin_descriptionDiffers() {
        catalog.createProduct(
                userId,
                new ProductRequest(
                        "Nome Catalogo Trib",
                        null,
                        "7899988877766",
                        null,
                        null,
                        new BigDecimal("4.00"),
                        new BigDecimal("9.00"),
                        BigDecimal.ZERO));

        String xml =
                """
                <nfe>
                  <nNF>4004</nNF>
                  <xNome>Fornecedor Trib</xNome>
                  <chNFe>35200466666666666666550040000040044000000000</chNFe>
                  <prod>
                    <xProd>Descricao XML diferente do catalogo</xProd>
                    <cEAN>SEM GTIN</cEAN>
                    <cEANTrib>7899988877766</cEANTrib>
                    <qCom>2</qCom>
                    <vUnCom>3.00</vUnCom>
                  </prod>
                </nfe>
                """;

        importNfeUseCase.execute(userId, xml);

        List<ProductResponse> products = catalog.listProducts(userId);
        assertThat(products.stream().filter(p -> p.name().equals("Nome Catalogo Trib")))
                .hasSize(1);
        assertThat(products.stream().anyMatch(p -> p.name().equals("Descricao XML diferente do catalogo")))
                .isFalse();

        ProductResponse kept =
                products.stream()
                        .filter(p -> p.name().equals("Nome Catalogo Trib"))
                        .findFirst()
                        .orElseThrow();
        assertThat(saldo(kept.id())).isEqualByComparingTo(new BigDecimal("2"));
    }

    @Test
    void importNfe_eanMatchTakesPrecedenceOverNameMatch() {
        catalog.createProduct(
                userId,
                new ProductRequest(
                        "Produto Alpha Ean",
                        null,
                        "7891111122222",
                        null,
                        null,
                        new BigDecimal("2.00"),
                        new BigDecimal("5.00"),
                        BigDecimal.ZERO));
        catalog.createProduct(
                userId,
                new ProductRequest(
                        "Texto Igual Xml",
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal("1.00"),
                        new BigDecimal("3.00"),
                        BigDecimal.ZERO));

        String xml =
                """
                <nfe>
                  <nNF>5005</nNF>
                  <xNome>Forn</xNome>
                  <chNFe>35200555555555555555550050000050055000000000</chNFe>
                  <prod>
                    <xProd>Texto Igual Xml</xProd>
                    <cEAN>7891111122222</cEAN>
                    <qCom>4</qCom>
                    <vUnCom>1.50</vUnCom>
                  </prod>
                </nfe>
                """;

        importNfeUseCase.execute(userId, xml);

        ProductResponse alpha =
                catalog.listProducts(userId).stream()
                        .filter(p -> p.name().equals("Produto Alpha Ean"))
                        .findFirst()
                        .orElseThrow();
        ProductResponse nameOnly =
                catalog.listProducts(userId).stream()
                        .filter(p -> p.name().equals("Texto Igual Xml"))
                        .findFirst()
                        .orElseThrow();

        assertThat(saldo(alpha.id())).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(saldo(nameOnly.id())).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void importNfe_matchesExistingProductByEan_whenDescriptionDiffers() {
catalog.createProduct(
                userId,
                new ProductRequest(
                        "Nome CatÃ¡logo Original",
                        null,
                        "7891234567890",
                        null,
                        null,
                        new BigDecimal("5.00"),
                        new BigDecimal("10.00"),
                        BigDecimal.ZERO));

        String xml =
                """
                <nfe>
                  <nNF>3003</nNF>
                  <xNome>Fornecedor EAN</xNome>
                  <chNFe>35200377777777777777550030000030033000000000</chNFe>
                  <prod>
                    <xProd>Nome no XML diferente</xProd>
                    <cEAN>7891234567890</cEAN>
                    <qCom>4</qCom>
                    <vUnCom>3.00</vUnCom>
                  </prod>
                </nfe>
                """;

importNfeUseCase.execute(userId, xml);

        List<ProductResponse> products = catalog.listProducts(userId);
        assertThat(products.stream().filter(p -> p.name().equals("Nome CatÃ¡logo Original")))
                .hasSize(1);
        assertThat(products.stream().anyMatch(p -> p.name().equals("Nome no XML diferente")))
                .isFalse();

        ProductResponse kept =
                products.stream()
                        .filter(p -> p.name().equals("Nome CatÃ¡logo Original"))
                        .findFirst()
                        .orElseThrow();
        assertThat(saldo(kept.id())).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(kept.costPrice()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void importNfe_duplicateAccessKey_returns409_andDoesNotDoubleStock() {
        String xml =
                """
                <nfe>
                  <nNF>2002</nNF>
                  <xNome>Fornecedor Dup</xNome>
                  <chNFe>35200288888888888888550020000020022000000000</chNFe>
                  <prod>
                    <xProd>Item Dup</xProd>
                    <qCom>5</qCom>
                    <vUnCom>2.00</vUnCom>
                  </prod>
                </nfe>
                """;

importNfeUseCase.execute(userId, xml);
        ProductResponse product =
catalog.listProducts(userId).stream()
                        .filter(p -> p.name().equals("Item Dup"))
                        .findFirst()
                        .orElseThrow();
        BigDecimal saldoAfterFirst = saldo(product.id());

        assertThatThrownBy(() -> importNfeUseCase.execute(userId, xml))
                .isInstanceOf(DuplicateNfeAccessKeyException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.CONFLICT));

        assertThat(nfeEntryRepository.findAll()).hasSize(1);
        assertThat(saldo(product.id())).isEqualByComparingTo(saldoAfterFirst);
    }

    @Test
    void importNfe_duplicateXmlWithoutAccessKey_doesNotDoubleStock() {
        String xml =
                """
                <nfe>
                  <nNF>7707</nNF>
                  <xNome>Fornecedor Sem Chave</xNome>
                  <prod>
                    <xProd>Item Sem Chave</xProd>
                    <qCom>2</qCom>
                    <vUnCom>5.00</vUnCom>
                  </prod>
                </nfe>
                """;

        importNfeUseCase.execute(userId, xml);
        ProductResponse product =
                catalog.listProducts(userId).stream()
                        .filter(p -> p.name().equals("Item Sem Chave"))
                        .findFirst()
                        .orElseThrow();
        BigDecimal saldoAfterFirst = saldo(product.id());

        assertThatThrownBy(() -> importNfeUseCase.execute(userId, xml))
                .isInstanceOf(DuplicateNfeXmlContentException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.DUPLICATE_NFE_XML));

        assertThat(nfeEntryRepository.findAll().stream().filter(e -> "7707".equals(e.getNfeNumber())))
                .hasSize(1);
        assertThat(saldo(product.id())).isEqualByComparingTo(saldoAfterFirst);
    }

    @Test
    void createBrand_sameNameCaseInsensitive_returnsExistingBrand() {
        var first = catalog.createBrand(userId, new BrandRequest("Marca Unica"));
        var second = catalog.createBrand(userId, new BrandRequest(" marca unica "));
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.name()).isEqualTo("Marca Unica");
    }

    @Test
    void createProduct_unknownBrandId_returns404() {
        assertThatThrownBy(
                        () ->
                                catalog.createProduct(
                                        userId,
                                        new ProductRequest(
                                                "Sem Marca Valida",
                                                9_999_999L,
                                                null,
                                                null,
                                                null,
                                                BigDecimal.ONE,
                                                BigDecimal.TEN,
                                                BigDecimal.ZERO)))
                .isInstanceOf(BrandNotFoundException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.NOT_FOUND));
    }

    @Test
    void updateBrand_renamesAndTrimsName() {
        var created = catalog.createBrand(userId, new BrandRequest("Marca Orig"));
        var updated =
                catalog.updateBrand(userId, created.id(), new BrandRequest("  Novo Titulo  "));
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Novo Titulo");
    }

    @Test
    void updateBrand_duplicateNameOtherBrand_returns409() {
        var alpha = catalog.createBrand(userId, new BrandRequest("Alpha"));
        catalog.createBrand(userId, new BrandRequest("Beta"));
        assertThatThrownBy(
                        () -> catalog.updateBrand(userId, alpha.id(), new BrandRequest("BETA")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.CONFLICT));
    }

    @Test
    void deleteBrand_unknown_returns404() {
        assertThatThrownBy(() -> catalog.deleteBrand(userId, 99_999_999L))
                .isInstanceOf(BrandNotFoundException.class);
    }

    @Test
    void deleteBrand_productShowsNaoInformadaBrand() {
        var brand = catalog.createBrand(userId, new BrandRequest("Marca Para Apagar"));
        ProductResponse p =
                catalog.createProduct(
                        userId,
                        new ProductRequest(
                                "Prod Com Marca",
                                brand.id(),
                                null,
                                null,
                                null,
                                BigDecimal.ONE,
                                BigDecimal.TEN,
                                BigDecimal.ZERO));
        assertThat(p.brandName()).isEqualTo("Marca Para Apagar");

        catalog.deleteBrand(userId, brand.id());

        var page =
                catalog.searchProducts(userId, null, "Prod Com Marca", false, PageRequest.of(0, 20));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().brandName()).isEqualTo("Nao informada");
    }

    @Test
    void searchProducts_filterByBrandId() {
        var brand = catalog.createBrand(userId, new BrandRequest("MarcaZ"));
        catalog.createProduct(
                userId,
                new ProductRequest(
                        "ComMarca",
                        brand.id(),
                        null,
                        null,
                        null,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.ZERO));
        catalog.createProduct(
                userId,
                new ProductRequest(
                        "Outro",
                        null,
                        null,
                        null,
                        null,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.ZERO));

        var page = catalog.searchProducts(userId, brand.id(), null, false, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().name()).isEqualTo("ComMarca");
    }

    @Test
    void searchProducts_lowStockTrue_includesProductBelowMinimum() {
        ProductResponse product = criarProdutoComMinimo("AbaixoMin", new BigDecimal("10"));
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("3"), "setup"));

        var page = catalog.searchProducts(userId, null, null, true, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(ProductResponse::name).contains("AbaixoMin");
    }

    @Test
    void adjustStock_unknownProduct_returns404() {
        assertThatThrownBy(
                        () ->
        inventory.adjustStock(
                                        userId,
                                        new StockAdjustmentRequest(99999L, BigDecimal.ONE, "X")))
                .isInstanceOf(ProductNotFoundException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.NOT_FOUND));
    }

    @Test
    void adjustStock_reasonOnlyWhitespace_returnsBadRequest() {
        ProductResponse product = criarProdutoSimples("MotivoBranco", "1.00", "2.00", "0");
        assertThatThrownBy(
                        () ->
                                inventory.adjustStock(
                                        userId,
                                        new StockAdjustmentRequest(product.id(), BigDecimal.ONE, "   ")))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));
    }

    @Test
    void adjustStock_wouldLeaveBalanceNegative_returnsBadRequest() {
        ProductResponse product = criarProdutoSimples("SaldoMin", "1.00", "2.00", "0");
        inventory.adjustStock(
                userId, new StockAdjustmentRequest(product.id(), new BigDecimal("4"), "entrada inicial"));

        assertThatThrownBy(
                        () ->
                                inventory.adjustStock(
                                        userId,
                                        new StockAdjustmentRequest(
                                                product.id(), new BigDecimal("-10"), "saida grande")))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("negativo");

        assertThat(saldo(product.id())).isEqualByComparingTo(new BigDecimal("4"));
    }

    private ProductResponse criarProdutoSimples(
            String nome, String custo, String venda, String minimo) {
        return catalog.createProduct(
                userId,
                new ProductRequest(
                        nome,
                        null,
                        null,
                        null,
                        null,
                        new BigDecimal(custo),
                        new BigDecimal(venda),
                        new BigDecimal(minimo)));
    }

    private ProductResponse criarProdutoComMinimo(String nome, BigDecimal minimo) {
        return catalog.createProduct(
                userId,
                new ProductRequest(
                        nome,
                        null,
                        null,
                        null,
                        null,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        minimo));
    }

    private BigDecimal saldo(long productId) {
        return inventoryBalanceRepository
                .findByUser_IdAndProduct_Id(userId, productId)
                .map(b -> b.getQuantity())
                .orElse(BigDecimal.ZERO);
    }
}

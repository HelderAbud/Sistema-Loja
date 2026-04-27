package com.lojapp.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lojapp.entity.NfeEntry;
import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import com.lojapp.dto.nfe.NfeImportResponse;
import com.lojapp.repository.InventoryBalanceRepository;
import com.lojapp.repository.NfeEntryRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.SupplierRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.exception.domain.DuplicateNfeAccessKeyException;
import com.lojapp.application.nfe.ImportNfeUseCase;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class NfeImportStockIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("lojapp")
                    .withUsername("lojapp")
                    .withPassword("lojapp_test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("lojapp.jwt.secret", () -> "integration-test-secret-32-chars-min!!");
    }

    @Autowired private ImportNfeUseCase importNfeUseCase;
    @Autowired private UserRepository users;
    @Autowired private ProductRepository products;
    @Autowired private InventoryBalanceRepository inventoryBalances;
    @Autowired private NfeEntryRepository nfeEntries;
    @Autowired private SupplierRepository suppliers;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void importNfe_createsProductAndIncreasesBalance() {
        User u = new User();
        u.setEmail("nfe-int-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);

        String pad =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave = ("352001" + pad).substring(0, 44);
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfe>
                  <nNF>9002</nNF>
                  <xNome>Fornecedor Int</xNome>
                  <chNFe>%s</chNFe>
                  <prod>
                    <xProd>Produto Integracao</xProd>
                    <qCom>7</qCom>
                    <vUnCom>3.00</vUnCom>
                  </prod>
                </nfe>
                """
                        .formatted(chave);

        importNfeUseCase.execute(u.getId(), xml);

        var product =
                products
                        .findByUser_IdAndNameIgnoreCase(u.getId(), "Produto Integracao")
                        .orElseThrow();
        var balance =
                inventoryBalances
                        .findByUser_IdAndProduct_Id(u.getId(), product.getId())
                        .orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo(new BigDecimal("7"));
    }

    @Test
    void importNfe_persistsSupplierTaxId_whenEmitHasCnpj() {
        User u = new User();
        u.setEmail("nfe-tax-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);

        String pad =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave = ("352005" + pad).substring(0, 44);
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <nNF>9300</nNF>
                  <emit>
                    <CNPJ>55.444.333/0001-22</CNPJ>
                    <xNome>Industria Tax Ltda</xNome>
                  </emit>
                  <chNFe>%s</chNFe>
                  <prod>
                    <xProd>Item Com CNPJ Emitente</xProd>
                    <qCom>1</qCom>
                    <vUnCom>1.00</vUnCom>
                  </prod>
                </nfeProc>
                """
                        .formatted(chave);

        NfeImportResponse response = importNfeUseCase.execute(u.getId(), xml);

        NfeEntry entry = nfeEntries.findById(response.nfeEntryId()).orElseThrow();
        assertThat(entry.getSupplierTaxId()).isEqualTo("55444333000122");
        assertThat(entry.getSupplier()).isNotNull();
        assertThat(response.supplierId()).isEqualTo(entry.getSupplier().getId());
        assertThat(entry.getSupplier().getLegalName()).isEqualTo("Industria Tax Ltda");
    }

    @Test
    void importNfe_twoNotesSameEmitCnpj_reusesOneSupplier() {
        User u = new User();
        u.setEmail("nfe-same-emit-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);
        long uid = u.getId();

        String pad1 =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave1 = ("352006" + pad1).substring(0, 44);
        String pad2 =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave2 = ("352008" + pad2).substring(0, 44);

        String xml1 =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfe>
                  <nNF>9401</nNF>
                  <emit><CNPJ>66778899000100</CNPJ><xNome>Mesmo Emit</xNome></emit>
                  <chNFe>%s</chNFe>
                  <prod><xProd>P1</xProd><qCom>1</qCom><vUnCom>1</vUnCom></prod>
                </nfe>
                """
                        .formatted(chave1);
        String xml2 =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfe>
                  <nNF>9402</nNF>
                  <emit><CNPJ>66.778.899/0001-00</CNPJ><xNome>Mesmo Emit</xNome></emit>
                  <chNFe>%s</chNFe>
                  <prod><xProd>P2</xProd><qCom>1</qCom><vUnCom>1</vUnCom></prod>
                </nfe>
                """
                        .formatted(chave2);

        NfeImportResponse r1 = importNfeUseCase.execute(uid, xml1);
        NfeImportResponse r2 = importNfeUseCase.execute(uid, xml2);

        assertThat(r1.supplierId()).isNotNull();
        assertThat(r2.supplierId()).isEqualTo(r1.supplierId());
        assertThat(suppliers.findByUser_IdOrderByLegalNameAsc(uid)).hasSize(1);
    }

    @Test
    void importNfe_sameAccessKeySecondTime_throwsDuplicate() {
        User u = new User();
        u.setEmail("nfe-dup-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);
        final long userId = u.getId();

        String pad =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave = ("352004" + pad).substring(0, 44);
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfe>
                  <nNF>9003</nNF>
                  <xNome>Fornecedor Dup</xNome>
                  <chNFe>%s</chNFe>
                  <prod>
                    <xProd>Produto Dup</xProd>
                    <qCom>1</qCom>
                    <vUnCom>1.00</vUnCom>
                  </prod>
                </nfe>
                """
                        .formatted(chave);

        importNfeUseCase.execute(userId, xml);
        assertThatThrownBy(() -> importNfeUseCase.execute(userId, xml))
                .isInstanceOf(DuplicateNfeAccessKeyException.class);
    }

    @Test
    void importNfe_namespacedXml_increasesBalance() {
        User u = new User();
        u.setEmail("nfe-ns-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);

        String pad =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave = ("352002" + pad).substring(0, 44);
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <nNF>9100</nNF>
                  <emit><xNome>Fornecedor NS</xNome></emit>
                  <chNFe>%s</chNFe>
                  <prod>
                    <xProd>Produto Com Namespace</xProd>
                    <qCom>4</qCom>
                    <vUnCom>11.50</vUnCom>
                  </prod>
                </nfeProc>
                """
                        .formatted(chave);

        importNfeUseCase.execute(u.getId(), xml);

        var product =
                products
                        .findByUser_IdAndNameIgnoreCase(u.getId(), "Produto Com Namespace")
                        .orElseThrow();
        var balance =
                inventoryBalances
                        .findByUser_IdAndProduct_Id(u.getId(), product.getId())
                        .orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo(new BigDecimal("4"));
    }

    /**
     * Layout prÃ³ximo do real: vÃ¡rios {@code det}, {@code cEAN} como SEM GTIN ou ausente â€” match por nome,
     * duas linhas no mesmo fluxo de importaÃ§Ã£o (guia Parte 7.3 Passo 5).
     */
    @Test
    void importNfe_namespacedXml_multipleItems_emptyOrSemGtinCean_increasesBothBalances() {
        User u = new User();
        u.setEmail("nfe-multi-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);

        String pad =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave = ("352003" + pad).substring(0, 44);
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <nNF>9201</nNF>
                  <emit><xNome>Fornecedor Multi Item</xNome></emit>
                  <chNFe>%s</chNFe>
                  <det nItem="1">
                    <prod>
                      <cEAN>SEM GTIN</cEAN>
                      <xProd>Produto Linha Um Piloto</xProd>
                      <qCom>3</qCom>
                      <vUnCom>10.00</vUnCom>
                    </prod>
                  </det>
                  <det nItem="2">
                    <prod>
                      <xProd>Produto Linha Dois Sem Tag CEAN</xProd>
                      <qCom>5</qCom>
                      <vUnCom>2.50</vUnCom>
                    </prod>
                  </det>
                </nfeProc>
                """
                        .formatted(chave);

        importNfeUseCase.execute(u.getId(), xml);

        var productOne =
                products
                        .findByUser_IdAndNameIgnoreCase(u.getId(), "Produto Linha Um Piloto")
                        .orElseThrow();
        var balanceOne =
                inventoryBalances
                        .findByUser_IdAndProduct_Id(u.getId(), productOne.getId())
                        .orElseThrow();
        assertThat(balanceOne.getQuantity()).isEqualByComparingTo(new BigDecimal("3"));

        var productTwo =
                products
                        .findByUser_IdAndNameIgnoreCase(
                                u.getId(), "Produto Linha Dois Sem Tag CEAN")
                        .orElseThrow();
        var balanceTwo =
                inventoryBalances
                        .findByUser_IdAndProduct_Id(u.getId(), productTwo.getId())
                        .orElseThrow();
        assertThat(balanceTwo.getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
    }

    /**
     * GTIN sÃ³ em {@code cEANTrib} com {@code cEAN}=SEM GTIN â€” o serviÃ§o deve fazer match por EAN
     * normalizado, nÃ£o criar produto novo pelo {@code xProd}.
     */
    @Test
    void importNfe_namespacedXml_ceanTribMatchesPreRegisteredProduct() {
        User u = new User();
        u.setEmail("nfe-trib-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash(passwordEncoder.encode("secret123"));
        u = users.save(u);
        long uid = u.getId();

        var existing = new Product();
        existing.setUser(u);
        existing.setName("SKU Trib Pre Cadastro");
        existing.setEan("7895566677788");
        existing.setCostPrice(new BigDecimal("10.00"));
        existing.setSalePrice(new BigDecimal("12.00"));
        existing.setMinimumStock(BigDecimal.ZERO);
        existing = products.save(existing);

        String pad =
                (UUID.randomUUID().toString().replace("-", "")
                                + UUID.randomUUID().toString().replace("-", ""))
                        .substring(0, 38);
        String chave = ("352006" + pad).substring(0, 44);
        String xml =
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <nfeProc xmlns="http://www.portalfiscal.inf.br/nfe">
                  <nNF>9302</nNF>
                  <emit><xNome>Fornecedor CEANTrib</xNome></emit>
                  <chNFe>%s</chNFe>
                  <det nItem="1">
                    <prod>
                      <xProd>Descricao NF diferente do catalogo</xProd>
                      <cEAN>SEM GTIN</cEAN>
                      <cEANTrib>7895566677788</cEANTrib>
                      <qCom>6</qCom>
                      <vUnCom>2.00</vUnCom>
                    </prod>
                  </det>
                </nfeProc>
                """
                        .formatted(chave);

        importNfeUseCase.execute(uid, xml);

        assertThat(products.findByUser_IdAndNameIgnoreCase(uid, "Descricao NF diferente do catalogo"))
                .isEmpty();
        var balance =
                inventoryBalances
                        .findByUser_IdAndProduct_Id(uid, existing.getId())
                        .orElseThrow();
        assertThat(balance.getQuantity()).isEqualByComparingTo(new BigDecimal("6"));
    }
}

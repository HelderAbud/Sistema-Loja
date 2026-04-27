package com.lojapp.application.nfe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.exception.domain.LojappDomainException;
import com.lojapp.repository.NfeEntryRepository;
import com.lojapp.repository.NfeItemRepository;
import com.lojapp.repository.ProductRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.observability.LojappBusinessMetrics;
import com.lojapp.service.AuditService;
import com.lojapp.service.InventoryService;
import com.lojapp.service.LojappHierarchyService;
import com.lojapp.service.NfeImportValidator;
import com.lojapp.service.NfeProductResolver;
import com.lojapp.service.NfeRawXmlStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportNfeUseCaseTest {

    @Mock private ProductRepository products;
    @Mock private UserRepository users;
    @Mock private NfeEntryRepository nfeEntries;
    @Mock private NfeItemRepository nfeItems;
    @Mock private InventoryService inventoryService;
    @Mock private AuditService auditService;
    @Mock private LojappHierarchyService hierarchyService;
    @Mock private NfeProductResolver productResolver;
    @Mock private NfeRawXmlStorage rawXmlStorage;
    @Mock private LojappBusinessMetrics businessMetrics;

    private ImportNfeUseCase importNfeUseCase;
    private NfeImportValidator importValidator;

    @BeforeEach
    void setUp() {
        importValidator = new NfeImportValidator(2000, 2);
        importNfeUseCase =
                new ImportNfeUseCase(
                        users,
                        nfeEntries,
                        nfeItems,
                        inventoryService,
                        auditService,
                        hierarchyService,
                        importValidator,
                        productResolver,
                        rawXmlStorage,
                        businessMetrics);
    }

    @Test
    void execute_blankRawXml_rejectsWithBadRequest() {
        assertThatThrownBy(() -> importNfeUseCase.execute(1L, "   "))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));

        verifyNoInteractions(users, nfeEntries, nfeItems, products, inventoryService, auditService);
    }

    @Test
    void execute_xmlLargerThanConfiguredLimit_rejectsWithBadRequest() {
        String rawXml = "<nfe>" + "x".repeat(2500) + "</nfe>";

        assertThatThrownBy(() -> importNfeUseCase.execute(1L, rawXml))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST));

        verifyNoInteractions(users, nfeEntries, nfeItems, products, inventoryService, auditService);
    }

    @Test
    void execute_withoutAnyProdNode_rejectsAsNoValidItems() {
        String xml =
                """
                <nfe>
                  <nNF>100</nNF>
                  <xNome>Fornecedor</xNome>
                  <chNFe>35200111111111111111550010000010011000000000</chNFe>
                </nfe>
                """;

        assertThatThrownBy(() -> importNfeUseCase.execute(1L, xml))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST))
                .hasMessageContaining("sem itens");

        verifyNoInteractions(users, nfeEntries, nfeItems, products, inventoryService, auditService);
    }

    @Test
    void execute_itemCountAboveConfiguredLimit_rejectsWithBadRequest() {
        String xml =
                """
                <nfe>
                  <nNF>101</nNF>
                  <xNome>Fornecedor</xNome>
                  <chNFe>35200111111111111111550010000010011000000001</chNFe>
                  <prod><xProd>Item 1</xProd><qCom>1</qCom><vUnCom>1.00</vUnCom></prod>
                  <prod><xProd>Item 2</xProd><qCom>1</qCom><vUnCom>1.00</vUnCom></prod>
                  <prod><xProd>Item 3</xProd><qCom>1</qCom><vUnCom>1.00</vUnCom></prod>
                </nfe>
                """;

        assertThatThrownBy(() -> importNfeUseCase.execute(1L, xml))
                .isInstanceOf(LojappDomainException.class)
                .satisfies(
                        ex ->
                                assertThat(((LojappDomainException) ex).getErrorCode())
                                        .isEqualTo(ApiErrorCode.BAD_REQUEST))
                .hasMessageContaining("limite");

        verifyNoInteractions(users, nfeEntries, nfeItems, products, inventoryService, auditService);
    }
}

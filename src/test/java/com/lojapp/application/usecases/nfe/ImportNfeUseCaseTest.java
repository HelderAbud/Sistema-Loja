package com.lojapp.application.usecases.nfe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lojapp.dto.ApiErrorCode;
import com.lojapp.entity.NfeEntry;
import com.lojapp.entity.Product;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.DuplicateNfeXmlContentException;
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
import com.lojapp.service.NfeProductResolver.ProductImportResolution;
import com.lojapp.service.NfeRawXmlStorage;
import com.lojapp.util.NfeXmlFingerprint;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Test
    void execute_xmlWithoutAccessKey_persistsNullKeyAndFingerprint() {
        stubSuccessfulImport();
        String xml = xmlWithoutKey("7701", "Item Sem Chave A");

        importNfeUseCase.execute(1L, xml);

        ArgumentCaptor<NfeEntry> captor = ArgumentCaptor.forClass(NfeEntry.class);
        verify(nfeEntries).save(captor.capture());
        assertThat(captor.getValue().getAccessKey()).isNull();
        assertThat(captor.getValue().getContentFingerprint())
                .isEqualTo(NfeXmlFingerprint.sha256Hex(xml));
        verify(nfeEntries, never()).existsByUser_IdAndAccessKey(anyLong(), any());
    }

    @Test
    void execute_sameXmlWithoutAccessKey_differentLineEndings_isDuplicate() {
        String lf = xmlWithoutKey("7702", "Item EOL");
        String crlf = lf.replace("\n", "\r\n");
        when(nfeEntries.existsByUser_IdAndContentFingerprint(
                        eq(1L), eq(NfeXmlFingerprint.sha256Hex(crlf))))
                .thenReturn(true);

        assertThatThrownBy(() -> importNfeUseCase.execute(1L, crlf))
                .isInstanceOf(DuplicateNfeXmlContentException.class);
        verify(nfeEntries, never()).save(any());
    }

    private void stubSuccessfulImport() {
        User user = new User();
        user.setId(1L);
        when(users.getReferenceById(1L)).thenReturn(user);
        when(nfeEntries.existsByUser_IdAndContentFingerprint(eq(1L), any())).thenReturn(false);
        when(hierarchyService.resolveSupplierForNfeImport(eq(1L), any(), any())).thenReturn(null);
        when(productResolver.suggestBrand(eq(1L), any())).thenReturn(Optional.empty());
        Product product = new Product();
        product.setId(10L);
        when(productResolver.resolveProductForImport(eq(1L), eq(user), any(), any()))
                .thenReturn(new ProductImportResolution(product, false));
        when(rawXmlStorage.persist(eq(1L), any()))
                .thenAnswer(inv -> new NfeRawXmlStorage.StoredRawXml(inv.getArgument(1), null));
        when(nfeEntries.save(any(NfeEntry.class)))
                .thenAnswer(
                        inv -> {
                            NfeEntry entry = inv.getArgument(0);
                            entry.setId(99L);
                            return entry;
                        });
    }

    private static String xmlWithoutKey(String number, String itemName) {
        return """
                <nfe>
                  <nNF>%s</nNF>
                  <xNome>Fornecedor Sem Chave</xNome>
                  <prod>
                    <xProd>%s</xProd>
                    <qCom>1</qCom>
                    <vUnCom>5.00</vUnCom>
                  </prod>
                </nfe>
                """
                .formatted(number, itemName);
    }
}

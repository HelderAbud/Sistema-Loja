package com.lojapp.application.nfe;

import com.lojapp.domain.nfe.NfeStockReceiptLine;
import com.lojapp.dto.nfe.NfeImportResponse;
import com.lojapp.entity.NfeEntry;
import com.lojapp.entity.NfeItem;
import com.lojapp.entity.Product;
import com.lojapp.entity.Supplier;
import com.lojapp.entity.User;
import com.lojapp.exception.domain.DuplicateNfeAccessKeyException;
import com.lojapp.exception.domain.DuplicateNfeXmlContentException;
import com.lojapp.repository.NfeEntryRepository;
import com.lojapp.repository.NfeItemRepository;
import com.lojapp.repository.UserRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.LojappHierarchyService;
import com.lojapp.service.NfeImportValidator;
import com.lojapp.service.NfeProductResolver;
import com.lojapp.service.NfeRawXmlStorage;
import com.lojapp.service.NfeXmlParser;
import com.lojapp.service.NfeXmlParser.ParsedNfe;
import com.lojapp.service.NfeXmlParser.ParsedNfeItem;
import com.lojapp.service.contract.InventoryServiceContract;
import com.lojapp.observability.LojappBusinessMetrics;
import com.lojapp.util.NfeBrandSuggester.BrandCandidate;
import com.lojapp.util.TokenHashUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: importar NFe a partir de XML bruto — persistência da entrada, linhas, stock e auditoria
 * na mesma transacção.
 */
@Service
public class ImportNfeUseCase {

    private static final Logger log = LoggerFactory.getLogger(ImportNfeUseCase.class);

    private final UserRepository users;
    private final NfeEntryRepository nfeEntries;
    private final NfeItemRepository nfeItems;
    private final InventoryServiceContract inventoryService;
    private final AuditService auditService;
    private final LojappHierarchyService hierarchyService;
    private final NfeImportValidator importValidator;
    private final NfeProductResolver productResolver;
    private final NfeRawXmlStorage rawXmlStorage;
    private final LojappBusinessMetrics businessMetrics;

    public ImportNfeUseCase(
            UserRepository users,
            NfeEntryRepository nfeEntries,
            NfeItemRepository nfeItems,
            InventoryServiceContract inventoryService,
            AuditService auditService,
            LojappHierarchyService hierarchyService,
            NfeImportValidator importValidator,
            NfeProductResolver productResolver,
            NfeRawXmlStorage rawXmlStorage,
            LojappBusinessMetrics businessMetrics) {
        this.users = users;
        this.nfeEntries = nfeEntries;
        this.nfeItems = nfeItems;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.hierarchyService = hierarchyService;
        this.importValidator = importValidator;
        this.productResolver = productResolver;
        this.rawXmlStorage = rawXmlStorage;
        this.businessMetrics = businessMetrics;
    }

    @Transactional
    public NfeImportResponse execute(long userId, String rawXml) {
        importValidator.validateRawXml(rawXml);
        ParsedNfe parsed = NfeXmlParser.parse(rawXml);
        importValidator.validateParsedItems(parsed.items().size());
        User user = users.getReferenceById(userId);

        String accessKey = parsed.accessKey() == null ? "" : parsed.accessKey().trim();
        String xmlFingerprint = TokenHashUtil.sha256Hex(rawXml);
        if (!accessKey.isEmpty()
                && nfeEntries.existsByUser_IdAndAccessKey(userId, accessKey)) {
            log.warn("Importação NFe recusada: chave duplicada userId={}", userId);
            businessMetrics.recordNfeImportDuplicateKey();
            throw new DuplicateNfeAccessKeyException();
        }
        if (accessKey.isEmpty()
                && nfeEntries.existsByUser_IdAndContentFingerprint(userId, xmlFingerprint)) {
            log.warn("Importação NFe recusada: XML duplicado (sem chave) userId={}", userId);
            businessMetrics.recordNfeImportDuplicateXml();
            throw new DuplicateNfeXmlContentException();
        }

        Supplier supplier =
                hierarchyService.resolveSupplierForNfeImport(
                        userId, parsed.supplierTaxId(), parsed.supplierName());

        Optional<BrandCandidate> brandPick = suggestBrand(userId, parsed);

        NfeEntry entry = new NfeEntry();
        entry.setUser(user);
        entry.setNfeNumber(parsed.number());
        entry.setSupplierName(parsed.supplierName());
        entry.setSupplierTaxId(parsed.supplierTaxId().orElse(null));
        entry.setSupplier(supplier);
        entry.setAccessKey(parsed.accessKey());
        if (accessKey.isEmpty()) {
            entry.setContentFingerprint(xmlFingerprint);
        }
        NfeRawXmlStorage.StoredRawXml storedRawXml = rawXmlStorage.persist(userId, rawXml);
        entry.setRawXml(storedRawXml.rawXml());
        entry.setRawXmlKey(storedRawXml.rawXmlKey());
        entry = nfeEntries.save(entry);

        List<NfeItem> lines = new ArrayList<>(parsed.items().size());
        int productsCreatedWithoutSalePrice = 0;
        for (ParsedNfeItem item : parsed.items()) {
            var resolution = productResolver.resolveProductForImport(userId, user, item, supplier);
            if (resolution.createdFallbackWithoutSalePrice()) {
                productsCreatedWithoutSalePrice++;
            }
            Product product = resolution.product();

            NfeStockReceiptLine stockLine = NfeStockReceiptLine.of(item.quantity());

            NfeItem nfeItem = new NfeItem();
            nfeItem.setNfeEntry(entry);
            nfeItem.setProduct(product);
            nfeItem.setDescription(item.description());
            nfeItem.setQuantity(stockLine.quantity());
            nfeItem.setUnitCost(item.unitCost());
            nfeItem.setLineTotal(stockLine.quantity().multiply(item.unitCost()));
            lines.add(nfeItem);

            inventoryService.increaseFromNfe(user, product, stockLine.quantity(), entry.getId());
        }
        nfeItems.saveAll(lines);
        int importedItems = lines.size();

        log.info(
                "NFe importada userId={} nfeEntryId={} nfeNumber={} itens={}",
                userId,
                entry.getId(),
                entry.getNfeNumber(),
                importedItems);
        businessMetrics.recordNfeImportSuccess();
        auditService.log(
                userId,
                "NFE_IMPORT",
                "nfeEntryId=%d nfeNumber=%s items=%d"
                        .formatted(entry.getId(), entry.getNfeNumber(), importedItems));

        Long supplierId = supplier == null ? null : supplier.getId();
        Long suggestedBrandId = brandPick.map(BrandCandidate::id).orElse(null);
        String suggestedBrandName = brandPick.map(BrandCandidate::name).orElse(null);
        return new NfeImportResponse(
                entry.getId(),
                entry.getNfeNumber(),
                importedItems,
                supplierId,
                suggestedBrandId,
                suggestedBrandName,
                productsCreatedWithoutSalePrice);
    }

    private Optional<BrandCandidate> suggestBrand(long userId, ParsedNfe parsed) {
        return productResolver.suggestBrand(userId, parsed);
    }
}

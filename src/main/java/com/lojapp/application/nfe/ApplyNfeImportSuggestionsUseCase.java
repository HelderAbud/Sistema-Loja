package com.lojapp.application.nfe;

import com.lojapp.dto.nfe.NfeApplySuggestionsRequest;
import com.lojapp.dto.nfe.NfeApplySuggestionsResponse;
import com.lojapp.entity.Brand;
import com.lojapp.entity.NfeEntry;
import com.lojapp.entity.NfeItem;
import com.lojapp.entity.Supplier;
import com.lojapp.exception.domain.NfeEntryNotFoundException;
import com.lojapp.repository.NfeEntryRepository;
import com.lojapp.repository.NfeItemRepository;
import com.lojapp.service.AuditService;
import com.lojapp.service.NfeProductResolver;
import com.lojapp.service.NfeProductResolver.SuggestionApplyResult;
import com.lojapp.service.NfeRawXmlStorage;
import com.lojapp.service.NfeXmlParser;
import com.lojapp.service.NfeXmlParser.ParsedNfe;
import com.lojapp.util.NfeBrandSuggester.BrandCandidate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: reler o XML de uma entrada importada e aplicar sugestões de marca/fornecedor aos produtos
 * ligados, sem sobrescrever valores já preenchidos.
 */
@Service
public class ApplyNfeImportSuggestionsUseCase {

    private final NfeEntryRepository nfeEntries;
    private final NfeItemRepository nfeItems;
    private final AuditService auditService;
    private final NfeProductResolver productResolver;
    private final NfeRawXmlStorage rawXmlStorage;

    public ApplyNfeImportSuggestionsUseCase(
            NfeEntryRepository nfeEntries,
            NfeItemRepository nfeItems,
            AuditService auditService,
            NfeProductResolver productResolver,
            NfeRawXmlStorage rawXmlStorage) {
        this.nfeEntries = nfeEntries;
        this.nfeItems = nfeItems;
        this.auditService = auditService;
        this.productResolver = productResolver;
        this.rawXmlStorage = rawXmlStorage;
    }

    @Transactional
    public NfeApplySuggestionsResponse execute(
            long userId, long nfeEntryId, NfeApplySuggestionsRequest request) {
        boolean doBrand = applyBrandFlag(request);
        boolean doSupplier = applySupplierFlag(request);

        NfeEntry entry =
                nfeEntries.findByIdAndUser_Id(nfeEntryId, userId).orElseThrow(NfeEntryNotFoundException::new);

        ParsedNfe parsed = NfeXmlParser.parse(rawXmlStorage.retrieve(entry.getRawXml(), entry.getRawXmlKey()));
        Optional<BrandCandidate> brandPick = suggestBrand(userId, parsed);
        Supplier entrySupplier = entry.getSupplier();

        List<NfeItem> lines = nfeItems.findByNfeEntryIdAndUserId(nfeEntryId, userId);
        Brand brandEntity =
                doBrand
                        ? productResolver.resolveSuggestedBrandEntity(userId, brandPick).orElse(null)
                        : null;
        SuggestionApplyResult applyResult =
                productResolver.applySuggestions(
                        lines, doBrand, brandEntity, doSupplier, entrySupplier);

        auditService.log(
                userId,
                "NFE_APPLY_SUGGESTIONS",
                "nfeEntryId=%d lines=%d brand=%d supplier=%d skippedModel=%d"
                        .formatted(
                                nfeEntryId,
                                lines.size(),
                                applyResult.brandAssigned(),
                                applyResult.supplierAssigned(),
                                applyResult.brandSkippedModel()));

        Long appliedBrandId = brandEntity == null ? null : brandEntity.getId();
        String appliedBrandName = brandEntity == null ? null : brandEntity.getName();
        Long supplierId = entrySupplier == null ? null : entrySupplier.getId();

        return new NfeApplySuggestionsResponse(
                lines.size(),
                applyResult.brandAssigned(),
                applyResult.supplierAssigned(),
                applyResult.brandSkippedModel(),
                appliedBrandId,
                appliedBrandName,
                supplierId);
    }

    private static boolean applyBrandFlag(NfeApplySuggestionsRequest r) {
        if (r == null || r.setBrandOnImportedProducts() == null) {
            return true;
        }
        return r.setBrandOnImportedProducts();
    }

    private static boolean applySupplierFlag(NfeApplySuggestionsRequest r) {
        if (r == null || r.setSupplierOnImportedProducts() == null) {
            return true;
        }
        return r.setSupplierOnImportedProducts();
    }

    private Optional<BrandCandidate> suggestBrand(long userId, ParsedNfe parsed) {
        return productResolver.suggestBrand(userId, parsed);
    }
}

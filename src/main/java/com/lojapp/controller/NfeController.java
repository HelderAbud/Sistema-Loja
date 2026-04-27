package com.lojapp.controller;

import com.lojapp.application.nfe.ApplyNfeImportSuggestionsUseCase;
import com.lojapp.application.nfe.ImportNfeUseCase;
import com.lojapp.dto.nfe.NfeApplySuggestionsRequest;
import com.lojapp.dto.nfe.NfeApplySuggestionsResponse;
import com.lojapp.dto.nfe.NfeImportRequest;
import com.lojapp.dto.nfe.NfeImportResponse;
import com.lojapp.security.JwtUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lojapp")
@Tag(name = "LojApp - NFe")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('USER','ADMIN','REPRESENTATIVE')")
public class NfeController {

    private final ImportNfeUseCase importNfeUseCase;
    private final ApplyNfeImportSuggestionsUseCase applyNfeImportSuggestionsUseCase;

    public NfeController(
            ImportNfeUseCase importNfeUseCase,
            ApplyNfeImportSuggestionsUseCase applyNfeImportSuggestionsUseCase) {
        this.importNfeUseCase = importNfeUseCase;
        this.applyNfeImportSuggestionsUseCase = applyNfeImportSuggestionsUseCase;
    }

    @PostMapping("/nfe/import")
    public NfeImportResponse importNfe(
            @Valid @RequestBody NfeImportRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return importNfeUseCase.execute(principal.userId(), request.rawXml());
    }

    @Operation(
            summary = "Aplicar sugestoes de uma NFe importada",
            description =
                    "Re-le o XML da entrada e aplica marca sugerida e/ou fornecedor da nota aos produtos "
                            + "ligados a essa importacao, apenas onde marca ou fornecedor ainda estao vazios.")
    @PostMapping("/nfe/entries/{nfeEntryId}/apply-suggestions")
    public NfeApplySuggestionsResponse applyImportSuggestions(
            @Parameter(description = "Id da entrada devolvido em importNfe") @PathVariable long nfeEntryId,
            @RequestBody(required = false) NfeApplySuggestionsRequest request,
            @AuthenticationPrincipal JwtUser principal) {
        return applyNfeImportSuggestionsUseCase.execute(principal.userId(), nfeEntryId, request);
    }
}

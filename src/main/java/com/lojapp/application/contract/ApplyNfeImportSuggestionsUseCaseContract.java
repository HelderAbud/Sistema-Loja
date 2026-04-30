package com.lojapp.application.contract;

import com.lojapp.dto.nfe.NfeApplySuggestionsRequest;
import com.lojapp.dto.nfe.NfeApplySuggestionsResponse;

public interface ApplyNfeImportSuggestionsUseCaseContract {

    NfeApplySuggestionsResponse execute(long userId, long nfeEntryId, NfeApplySuggestionsRequest request);
}

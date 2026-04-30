package com.lojapp.application.contract;

import com.lojapp.dto.nfe.NfeImportResponse;

public interface ImportNfeUseCaseContract {

    NfeImportResponse execute(long userId, String rawXml);
}

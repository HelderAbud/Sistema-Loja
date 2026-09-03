package com.lojapp.exception.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lojapp.dto.ApiErrorCode;
import org.junit.jupiter.api.Test;

class CashSessionManagerApprovalRequiredExceptionTest {

    @Test
    void messageDescribesSelfAcknowledgementNotThirdPartyManager() {
        var ex = new CashSessionManagerApprovalRequiredException();
        assertThat(ex.getErrorCode()).isEqualTo(ApiErrorCode.FORBIDDEN);
        assertThat(ex.getMessage()).contains("confirmação de revisão").doesNotContain("gestor");
    }
}

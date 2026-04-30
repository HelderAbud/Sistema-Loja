package com.lojapp.dto.sale;

import com.lojapp.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PosSalePaymentRequest(
        @NotNull PaymentMethod paymentMethod, @NotNull @DecimalMin("0.01") BigDecimal amount) {}

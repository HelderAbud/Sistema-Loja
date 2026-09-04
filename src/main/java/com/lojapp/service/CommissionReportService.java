package com.lojapp.service;

import com.lojapp.dto.commission.CommissionAccrualResponse;
import com.lojapp.entity.CommissionAccrual;
import com.lojapp.repository.CommissionAccrualRepository;
import com.lojapp.service.contract.CommissionReportServiceContract;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommissionReportService implements CommissionReportServiceContract {

    private final CommissionAccrualRepository accruals;

    public CommissionReportService(CommissionAccrualRepository accruals) {
        this.accruals = accruals;
    }

    /** Accruals de vendas canceladas ficam de fora (filtro {@code cancelledAt} no repositório). */
    @Override
    @Transactional(readOnly = true)
    public List<CommissionAccrualResponse> list(long userId, Instant from, Instant to) {
        return accruals.findByUser_IdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, from, to).stream()
                .map(CommissionReportService::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String toCsv(long userId, Instant from, Instant to) {
        StringBuilder out = new StringBuilder();
        out.append("id,saleId,sellerId,sellerName,brandId,brandName,baseAmount,percent,amount,createdAt\n");
        for (CommissionAccrualResponse row : list(userId, from, to)) {
            out.append(row.id())
                    .append(',')
                    .append(row.saleId())
                    .append(',')
                    .append(row.sellerId())
                    .append(',')
                    .append(csvCell(row.sellerName()))
                    .append(',')
                    .append(row.brandId() == null ? "" : row.brandId())
                    .append(',')
                    .append(csvCell(row.brandName()))
                    .append(',')
                    .append(row.baseAmount().toPlainString())
                    .append(',')
                    .append(row.percent().toPlainString())
                    .append(',')
                    .append(row.amount().toPlainString())
                    .append(',')
                    .append(row.createdAt())
                    .append('\n');
        }
        return out.toString();
    }

    private static CommissionAccrualResponse toResponse(CommissionAccrual row) {
        return new CommissionAccrualResponse(
                row.getId(),
                row.getSale().getId(),
                row.getSeller().getId(),
                row.getSeller().getDisplayName(),
                row.getBrand() == null ? null : row.getBrand().getId(),
                row.getBrand() == null ? null : row.getBrand().getName(),
                row.getBaseAmount(),
                row.getPercent(),
                row.getAmount(),
                row.getCreatedAt());
    }

    private static final Set<Character> FORMULA_TRIGGERS = Set.of('=', '+', '-', '@', '\t', '\r');

    static String csvCell(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = value;
        if (FORMULA_TRIGGERS.contains(safe.charAt(0))) {
            safe = "'" + safe;
        }
        if (safe.indexOf(',') >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}

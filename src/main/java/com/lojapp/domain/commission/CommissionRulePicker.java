package com.lojapp.domain.commission;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Escolhe a regra vigente em {@code at}: marca ganha a global; entre iguais, {@code validFrom} mais recente. */
public final class CommissionRulePicker {

    private CommissionRulePicker() {}

    public static Optional<CommissionRuleSnapshot> pick(
            List<CommissionRuleSnapshot> rules, Long brandId, Instant at) {
        if (rules == null || rules.isEmpty() || at == null) {
            return Optional.empty();
        }
        List<CommissionRuleSnapshot> eligible =
                rules.stream().filter(r -> !r.validFrom().isAfter(at)).toList();
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        if (brandId != null) {
            Optional<CommissionRuleSnapshot> branded =
                    newest(eligible.stream().filter(r -> brandId.equals(r.brandId())).toList());
            if (branded.isPresent()) {
                return branded;
            }
        }
        return newest(eligible.stream().filter(r -> r.brandId() == null).toList());
    }

    private static Optional<CommissionRuleSnapshot> newest(List<CommissionRuleSnapshot> scoped) {
        return scoped.stream()
                .max(
                        Comparator.comparing(CommissionRuleSnapshot::validFrom)
                                .thenComparing(CommissionRuleSnapshot::id));
    }
}

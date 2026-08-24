package com.lojapp.domain.commission;

import java.util.List;

/** Round-robin determinístico: ordem estável de ids; desempate já vem na lista. */
public final class SellerRoundRobin {

    private SellerRoundRobin() {}

    public static Long nextId(List<Long> activeIdsInOrder, Long lastAssignedId) {
        if (activeIdsInOrder == null || activeIdsInOrder.isEmpty()) {
            return null;
        }
        if (lastAssignedId == null) {
            return activeIdsInOrder.get(0);
        }
        int index = activeIdsInOrder.indexOf(lastAssignedId);
        if (index < 0) {
            return activeIdsInOrder.get(0);
        }
        return activeIdsInOrder.get((index + 1) % activeIdsInOrder.size());
    }
}

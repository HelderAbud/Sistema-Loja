package com.lojapp.entity;

/**
 * Valores persistidos em {@code inventory_movements.movement_type} (varchar). Usar {@link #name()}
 * para manter compatibilidade com dados já gravados.
 */
public enum InventoryMovementType {
    SALE,
    ENTRY,
    ADJUSTMENT
}

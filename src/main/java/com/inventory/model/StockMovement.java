package com.inventory.model;

import java.time.Instant;

/** Immutable audit record. {@code delta} is signed: positive adds stock, negative removes it. */
public record StockMovement(Instant timestamp, String sku, MovementType type, int delta, String note) { }

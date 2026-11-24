package com.inventory.repository;

import com.inventory.model.StockMovement;
import java.util.ArrayList;
import java.util.List;

public class InMemoryMovementLog implements MovementLog {
    private final List<StockMovement> entries = new ArrayList<>();

    @Override public synchronized void append(StockMovement m) { entries.add(m); }
    @Override public synchronized List<StockMovement> history(String sku) {
        return entries.stream().filter(m -> m.sku().equals(sku)).toList();
    }
}

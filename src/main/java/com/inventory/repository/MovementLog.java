package com.inventory.repository;

import com.inventory.model.StockMovement;
import java.util.List;

public interface MovementLog {
    void append(StockMovement movement);
    List<StockMovement> history(String sku);
}

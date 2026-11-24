package com.inventory.alert;

import com.inventory.model.Product;

/** Observer notified when stock crosses down into the reorder threshold. */
public interface StockAlertListener {
    void onLowStock(Product product);
}

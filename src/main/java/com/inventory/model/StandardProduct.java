package com.inventory.model;

import java.math.BigDecimal;
import java.time.Clock;

public class StandardProduct extends Product {
    public StandardProduct(String sku, String name, Category category, BigDecimal unitPrice,
                           int quantity, int reorderLevel) {
        super(sku, name, category, unitPrice, quantity, reorderLevel);
    }

    @Override public boolean isSellable(Clock clock) { return true; }
    @Override public String type() { return "STANDARD"; }
    @Override public Product copy() {
        return new StandardProduct(sku(), name(), category(), unitPrice(), quantity(), reorderLevel());
    }
}

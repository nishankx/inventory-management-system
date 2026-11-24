package com.inventory.model;

import com.inventory.util.Validators;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/** A product with an expiry date; expired stock cannot be issued. */
public class PerishableProduct extends Product {
    private final LocalDate expiryDate;

    public PerishableProduct(String sku, String name, Category category, BigDecimal unitPrice,
                             int quantity, int reorderLevel, LocalDate expiryDate) {
        super(sku, name, category, unitPrice, quantity, reorderLevel);
        this.expiryDate = Validators.notNull(expiryDate, "expiryDate");
    }

    public LocalDate expiryDate() { return expiryDate; }

    @Override public boolean isSellable(Clock clock) { return !expiryDate.isBefore(LocalDate.now(clock)); }
    @Override public String type() { return "PERISHABLE"; }
    @Override public Product copy() {
        return new PerishableProduct(sku(), name(), category(), unitPrice(), quantity(), reorderLevel(), expiryDate);
    }
}

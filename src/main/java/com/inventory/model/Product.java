package com.inventory.model;

import com.inventory.exception.InsufficientStockException;
import com.inventory.exception.ValidationException;
import com.inventory.util.Validators;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

/**
 * Base product. State changes go through validated methods (encapsulation);
 * subclasses define what "sellable" means (polymorphism).
 */
public abstract class Product {
    private final String sku;
    private String name;
    private Category category;
    private BigDecimal unitPrice;
    private int quantity;
    private int reorderLevel;

    protected Product(String sku, String name, Category category, BigDecimal unitPrice,
                      int quantity, int reorderLevel) {
        this.sku = Validators.sku(sku);
        this.name = Validators.text(name, "name", 100);
        this.category = Validators.notNull(category, "category");
        this.unitPrice = Validators.money(unitPrice);
        this.quantity = Validators.nonNegative(quantity, "quantity");
        this.reorderLevel = Validators.nonNegative(reorderLevel, "reorderLevel");
    }

    public String sku() { return sku; }
    public String name() { return name; }
    public Category category() { return category; }
    public BigDecimal unitPrice() { return unitPrice; }
    public int quantity() { return quantity; }
    public int reorderLevel() { return reorderLevel; }

    public void rename(String newName) { this.name = Validators.text(newName, "name", 100); }
    public void changePrice(BigDecimal newPrice) { this.unitPrice = Validators.money(newPrice); }
    public void changeReorderLevel(int level) { this.reorderLevel = Validators.nonNegative(level, "reorderLevel"); }

    public void addStock(int qty) {
        Validators.positive(qty, "quantity");
        try {
            this.quantity = Math.addExact(this.quantity, qty);
        } catch (ArithmeticException e) {
            throw new ValidationException("Stock quantity would overflow");
        }
    }

    public void removeStock(int qty) {
        Validators.positive(qty, "quantity");
        if (qty > quantity) throw new InsufficientStockException(sku, quantity, qty);
        this.quantity -= qty;
    }

    public void setQuantity(int newQuantity) { this.quantity = Validators.nonNegative(newQuantity, "quantity"); }

    public BigDecimal totalValue() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }

    public boolean isLowStock() { return quantity <= reorderLevel; }

    /** Whether this product may currently be sold/issued. */
    public abstract boolean isSellable(Clock clock);

    /** Stable type tag used for persistence and display. */
    public abstract String type();

    /** Defensive copy so services can mutate safely and only commit on success. */
    public abstract Product copy();

    @Override public boolean equals(Object o) { return o instanceof Product p && sku.equals(p.sku); }
    @Override public int hashCode() { return Objects.hash(sku); }
    @Override public String toString() { return type() + "[" + sku + ", " + name + ", qty=" + quantity + "]"; }
}

package com.inventory.exception;

public class InsufficientStockException extends InventoryException {
    private final String sku;
    private final int available;
    private final int requested;

    public InsufficientStockException(String sku, int available, int requested) {
        super("Insufficient stock for " + sku + ": available=" + available + ", requested=" + requested);
        this.sku = sku;
        this.available = available;
        this.requested = requested;
    }

    public String sku() { return sku; }
    public int available() { return available; }
    public int requested() { return requested; }
}

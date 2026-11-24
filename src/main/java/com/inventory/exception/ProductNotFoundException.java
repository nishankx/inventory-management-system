package com.inventory.exception;

public class ProductNotFoundException extends InventoryException {
    public ProductNotFoundException(String sku) { super("Product not found: " + sku); }
}

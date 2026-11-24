package com.inventory.exception;

public class DuplicateSkuException extends InventoryException {
    public DuplicateSkuException(String sku) { super("SKU already exists: " + sku); }
}

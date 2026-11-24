package com.inventory.exception;

/** Base type for all domain errors; unchecked so callers choose where to handle them. */
public class InventoryException extends RuntimeException {
    public InventoryException(String message) { super(message); }
    public InventoryException(String message, Throwable cause) { super(message, cause); }
}

package com.inventory;

import static org.junit.jupiter.api.Assertions.*;

import com.inventory.exception.*;
import com.inventory.model.*;
import com.inventory.repository.*;
import com.inventory.service.InventoryService;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC);
    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(new InMemoryProductRepository(), new InMemoryMovementLog(), clock);
    }

    private Product widget(int qty, int reorder) {
        return new StandardProduct("w-1", "Widget", Category.HARDWARE, new BigDecimal("2.50"), qty, reorder);
    }

    @Test void registerNormalisesSkuAndRejectsDuplicates() {
        service.register(widget(10, 2));
        assertEquals("W-1", service.find("w-1").sku());
        assertThrows(DuplicateSkuException.class, () -> service.register(widget(1, 1)));
    }

    @Test void receiveAndIssueUpdateQuantityAndHistory() {
        service.register(widget(10, 2));
        service.receive("W-1", 5, "PO-1");
        service.issue("W-1", 3, "Order 7");
        assertEquals(12, service.find("W-1").quantity());
        assertEquals(3, service.history("W-1").size());
    }

    @Test void issueMoreThanAvailableFailsAndLeavesStockUnchanged() {
        service.register(widget(5, 1));
        assertThrows(InsufficientStockException.class, () -> service.issue("W-1", 6, "x"));
        assertEquals(5, service.find("W-1").quantity());
    }

    @Test void rejectsNonPositiveQuantities() {
        service.register(widget(5, 1));
        assertThrows(ValidationException.class, () -> service.receive("W-1", 0, ""));
        assertThrows(ValidationException.class, () -> service.issue("W-1", -2, ""));
    }

    @Test void lowStockAlertFiresOnlyWhenCrossingThreshold() {
        service.register(widget(5, 2));
        List<String> alerts = new ArrayList<>();
        service.addListener(p -> alerts.add(p.sku()));
        service.issue("W-1", 2, "");   // 3 left, still above
        assertTrue(alerts.isEmpty());
        service.issue("W-1", 1, "");   // 2 left, crosses
        service.issue("W-1", 1, "");   // already low, no repeat
        assertEquals(List.of("W-1"), alerts);
    }

    @Test void expiredPerishablesCannotBeIssued() {
        service.register(new PerishableProduct("MILK", "Milk", Category.GROCERY, new BigDecimal("1.20"),
                10, 2, LocalDate.of(2026, 11, 23)));
        assertThrows(InventoryException.class, () -> service.issue("MILK", 1, ""));
        assertEquals(1, service.expired().size());
    }

    @Test void adjustRecordsSignedDeltaAndRequiresReason() {
        service.register(widget(10, 2));
        service.adjust("W-1", 7, "Cycle count");
        assertEquals(-3, service.history("W-1").get(1).delta());
        assertThrows(ValidationException.class, () -> service.adjust("W-1", 7, " "));
    }

    @Test void valuationSumsAllProducts() {
        service.register(widget(10, 2));
        service.register(new StandardProduct("B", "Bolt", Category.HARDWARE, new BigDecimal("0.10"), 100, 5));
        assertEquals(new BigDecimal("35.00"), service.totalValuation());
    }

    @Test void cannotRemoveProductWithStock() {
        service.register(widget(1, 0));
        assertThrows(ValidationException.class, () -> service.remove("W-1"));
        service.issue("W-1", 1, "");
        assertTrue(service.remove("W-1"));
    }

    @Test void unknownSkuThrowsNotFound() {
        assertThrows(ProductNotFoundException.class, () -> service.find("NOPE"));
    }
}

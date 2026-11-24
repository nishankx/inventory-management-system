package com.inventory.service;

import com.inventory.alert.StockAlertListener;
import com.inventory.exception.DuplicateSkuException;
import com.inventory.exception.InventoryException;
import com.inventory.exception.ProductNotFoundException;
import com.inventory.exception.ValidationException;
import com.inventory.model.MovementType;
import com.inventory.model.Product;
import com.inventory.model.StockMovement;
import com.inventory.repository.MovementLog;
import com.inventory.repository.ProductRepository;
import com.inventory.util.Validators;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Business rules live here. Every operation: load a copy, mutate the copy, persist it,
 * then write the audit record. A failure before persisting leaves stored state unchanged.
 */
public class InventoryService {
    private static final Logger LOG = Logger.getLogger(InventoryService.class.getName());

    private final ProductRepository products;
    private final MovementLog movements;
    private final Clock clock;
    private final List<StockAlertListener> listeners = new CopyOnWriteArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public InventoryService(ProductRepository products, MovementLog movements, Clock clock) {
        this.products = Validators.notNull(products, "products");
        this.movements = Validators.notNull(movements, "movements");
        this.clock = Validators.notNull(clock, "clock");
    }

    public void addListener(StockAlertListener l) { listeners.add(l); }

    public Product register(Product product) {
        return locked(() -> {
            if (products.findBySku(product.sku()).isPresent()) throw new DuplicateSkuException(product.sku());
            products.save(product);
            if (product.quantity() > 0) {
                record(product.sku(), MovementType.INITIAL, product.quantity(), "Initial stock");
            }
            LOG.info(() -> "Registered " + product);
            return product.copy();
        });
    }

    public Product receive(String sku, int qty, String note) {
        return locked(() -> {
            Product p = require(sku);
            p.addStock(qty);
            products.save(p);
            record(p.sku(), MovementType.RECEIPT, qty, note);
            return p;
        });
    }

    public Product issue(String sku, int qty, String note) {
        return locked(() -> {
            Product p = require(sku);
            if (!p.isSellable(clock)) throw new InventoryException("Product " + p.sku() + " is not sellable (expired)");
            boolean wasLow = p.isLowStock();
            p.removeStock(qty);
            products.save(p);
            record(p.sku(), MovementType.ISSUE, -qty, note);
            if (!wasLow && p.isLowStock()) listeners.forEach(l -> l.onLowStock(p.copy()));
            return p;
        });
    }

    /** Set an absolute quantity (e.g. after a stock count). A reason is mandatory for the audit trail. */
    public Product adjust(String sku, int newQuantity, String reason) {
        String why = Validators.text(reason, "reason", 200);
        return locked(() -> {
            Product p = require(sku);
            int delta = newQuantity - p.quantity();
            p.setQuantity(newQuantity);
            products.save(p);
            record(p.sku(), MovementType.ADJUSTMENT, delta, why);
            return p;
        });
    }

    public Product find(String sku) { return locked(() -> require(sku)); }

    public List<Product> listAll() {
        return locked(() -> products.findAll().stream().sorted(Comparator.comparing(Product::sku)).toList());
    }

    public List<Product> lowStock() {
        return listAll().stream().filter(Product::isLowStock).toList();
    }

    public List<Product> expired() {
        return listAll().stream().filter(p -> !p.isSellable(clock)).toList();
    }

    public List<Product> search(String term) {
        String t = Validators.text(term, "search term", 100).toLowerCase(Locale.ROOT);
        return listAll().stream()
                .filter(p -> p.name().toLowerCase(Locale.ROOT).contains(t) || p.sku().toLowerCase(Locale.ROOT).contains(t))
                .toList();
    }

    public BigDecimal totalValuation() {
        return listAll().stream().map(Product::totalValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<StockMovement> history(String sku) {
        return locked(() -> {
            String key = Validators.sku(sku);
            require(key);
            return movements.history(key);
        });
    }

    public boolean remove(String sku) {
        return locked(() -> {
            Product p = require(sku);
            if (p.quantity() > 0) throw new ValidationException("Cannot remove " + p.sku() + " while stock is on hand");
            return products.delete(p.sku());
        });
    }

    private Product require(String sku) {
        String key = Validators.sku(sku);
        return products.findBySku(key).orElseThrow(() -> new ProductNotFoundException(key));
    }

    private void record(String sku, MovementType type, int delta, String note) {
        movements.append(new StockMovement(Instant.now(clock), sku, type, delta, note));
    }

    private <T> T locked(Supplier<T> action) {
        lock.lock();
        try { return action.get(); } finally { lock.unlock(); }
    }
}

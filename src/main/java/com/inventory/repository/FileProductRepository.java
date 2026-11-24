package com.inventory.repository;

import com.inventory.exception.RepositoryException;
import com.inventory.model.Category;
import com.inventory.model.PerishableProduct;
import com.inventory.model.Product;
import com.inventory.model.StandardProduct;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tab-separated file store. Writes go to a temp file then atomically replace the
 * real one, so a crash mid-write never leaves a half-written data file.
 * Field validation (no control chars in names) makes tabs safe as a delimiter.
 */
public class FileProductRepository implements ProductRepository {
    private final Path file;
    private final Map<String, Product> cache = new LinkedHashMap<>();

    public FileProductRepository(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        if (!Files.exists(file)) return;
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RepositoryException("Cannot read " + file, e);
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) continue;
            try {
                Product p = parse(line);
                cache.put(p.sku(), p);
            } catch (RuntimeException e) {
                throw new RepositoryException("Corrupt record at line " + (i + 1) + " of " + file, e);
            }
        }
    }

    private static Product parse(String line) {
        String[] f = line.split("\t", -1);
        if (f.length != 8) throw new IllegalArgumentException("expected 8 fields, got " + f.length);
        Category cat = Category.valueOf(f[3]);
        BigDecimal price = new BigDecimal(f[4]);
        int qty = Integer.parseInt(f[5]);
        int reorder = Integer.parseInt(f[6]);
        return switch (f[0]) {
            case "STANDARD" -> new StandardProduct(f[1], f[2], cat, price, qty, reorder);
            case "PERISHABLE" -> new PerishableProduct(f[1], f[2], cat, price, qty, reorder, LocalDate.parse(f[7]));
            default -> throw new IllegalArgumentException("unknown type " + f[0]);
        };
    }

    private static String format(Product p) {
        String expiry = p instanceof PerishableProduct pp ? pp.expiryDate().toString() : "";
        return String.join("\t", p.type(), p.sku(), p.name(), p.category().name(),
                p.unitPrice().toPlainString(), String.valueOf(p.quantity()),
                String.valueOf(p.reorderLevel()), expiry);
    }

    private void persist() {
        List<String> out = new ArrayList<>();
        cache.values().forEach(p -> out.add(format(p)));
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path tmp = Files.createTempFile(parent, "products", ".tmp");
            Files.write(tmp, out, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RepositoryException("Cannot write " + file, e);
        }
    }

    @Override public synchronized Optional<Product> findBySku(String sku) {
        return Optional.ofNullable(cache.get(sku)).map(Product::copy);
    }

    @Override public synchronized List<Product> findAll() {
        return cache.values().stream().map(Product::copy).toList();
    }

    @Override public synchronized void save(Product product) {
        Product previous = cache.put(product.sku(), product.copy());
        try {
            persist();
        } catch (RuntimeException e) {          // roll back in-memory state if disk write failed
            if (previous == null) cache.remove(product.sku()); else cache.put(product.sku(), previous);
            throw e;
        }
    }

    @Override public synchronized boolean delete(String sku) {
        Product removed = cache.remove(sku);
        if (removed == null) return false;
        try {
            persist();
        } catch (RuntimeException e) {
            cache.put(sku, removed);
            throw e;
        }
        return true;
    }
}

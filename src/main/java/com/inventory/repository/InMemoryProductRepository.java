package com.inventory.repository;

import com.inventory.model.Product;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryProductRepository implements ProductRepository {
    private final Map<String, Product> store = new LinkedHashMap<>();

    @Override public synchronized Optional<Product> findBySku(String sku) {
        return Optional.ofNullable(store.get(sku)).map(Product::copy);
    }
    @Override public synchronized List<Product> findAll() {
        return store.values().stream().map(Product::copy).toList();
    }
    @Override public synchronized void save(Product product) { store.put(product.sku(), product.copy()); }
    @Override public synchronized boolean delete(String sku) { return store.remove(sku) != null; }
}

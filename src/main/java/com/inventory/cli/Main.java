package com.inventory.cli;

import com.inventory.alert.LoggingAlertListener;
import com.inventory.exception.InventoryException;
import com.inventory.model.Category;
import com.inventory.model.PerishableProduct;
import com.inventory.model.Product;
import com.inventory.model.StandardProduct;
import com.inventory.model.StockMovement;
import com.inventory.repository.FileMovementLog;
import com.inventory.repository.FileProductRepository;
import com.inventory.service.InventoryService;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String HELP = """
            Commands:
              add <sku> <price> <qty> <reorder> <CATEGORY> [yyyy-MM-dd] <name...>
                  (give an expiry date to create a perishable product)
              receive <sku> <qty> [note...]
              issue   <sku> <qty> [note...]
              adjust  <sku> <newQty> <reason...>
              show <sku> | list | low | expired | search <text> | value | history <sku>
              remove <sku>      (only when stock is zero)
              help | quit
            Categories: ELECTRONICS GROCERY CLOTHING HARDWARE OTHER""";

    public static void main(String[] args) {
        Path dir = Path.of(args.length > 0 ? args[0] : "data");
        InventoryService service = new InventoryService(
                new FileProductRepository(dir.resolve("products.tsv")),
                new FileMovementLog(dir.resolve("movements.tsv")),
                Clock.systemDefaultZone());
        service.addListener(new LoggingAlertListener());

        System.out.println("Inventory Manager (data: " + dir.toAbsolutePath() + ")  type 'help'");
        try (Scanner in = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                if (!in.hasNextLine()) break;
                String line = in.nextLine().trim();
                if (line.isEmpty()) continue;
                if (line.equalsIgnoreCase("quit") || line.equalsIgnoreCase("exit")) break;
                try {
                    execute(service, line.split("\\s+"));
                } catch (InventoryException e) {
                    System.out.println("Error: " + e.getMessage());
                } catch (IllegalArgumentException | DateTimeParseException | IndexOutOfBoundsException e) {
                    System.out.println("Invalid input (type 'help'): " + e.getMessage());
                }
            }
        }
    }

    private static void execute(InventoryService s, String[] t) {
        switch (t[0].toLowerCase()) {
            case "help" -> System.out.println(HELP);
            case "add" -> add(s, t);
            case "receive" -> print(s.receive(t[1], Integer.parseInt(t[2]), rest(t, 3)));
            case "issue" -> print(s.issue(t[1], Integer.parseInt(t[2]), rest(t, 3)));
            case "adjust" -> print(s.adjust(t[1], Integer.parseInt(t[2]), rest(t, 3)));
            case "show" -> print(s.find(t[1]));
            case "list" -> table(s.listAll());
            case "low" -> table(s.lowStock());
            case "expired" -> table(s.expired());
            case "search" -> table(s.search(rest(t, 1)));
            case "value" -> System.out.println("Total inventory value: " + s.totalValuation());
            case "history" -> history(s.history(t[1]));
            case "remove" -> System.out.println(s.remove(t[1]) ? "Removed." : "Nothing removed.");
            default -> System.out.println("Unknown command. Type 'help'.");
        }
    }

    private static void add(InventoryService s, String[] t) {
        BigDecimal price = new BigDecimal(t[2]);
        int qty = Integer.parseInt(t[3]);
        int reorder = Integer.parseInt(t[4]);
        Category cat = Category.valueOf(t[5].toUpperCase());
        Product p;
        if (t.length > 6 && t[6].matches("\\d{4}-\\d{2}-\\d{2}")) {
            p = new PerishableProduct(t[1], rest(t, 7), cat, price, qty, reorder, LocalDate.parse(t[6]));
        } else {
            p = new StandardProduct(t[1], rest(t, 6), cat, price, qty, reorder);
        }
        print(s.register(p));
    }

    private static String rest(String[] t, int from) {
        return from >= t.length ? "" : String.join(" ", Arrays.copyOfRange(t, from, t.length));
    }

    private static void print(Product p) {
        System.out.printf("%s | %s | %s | price=%s | qty=%d | reorder=%d%s%n", p.sku(), p.type(), p.name(),
                p.unitPrice(), p.quantity(), p.reorderLevel(),
                p instanceof PerishableProduct pp ? " | expires=" + pp.expiryDate() : "");
    }

    private static void table(List<Product> products) {
        if (products.isEmpty()) { System.out.println("(none)"); return; }
        System.out.printf("%-14s %-28s %-11s %9s %6s %s%n", "SKU", "NAME", "TYPE", "PRICE", "QTY", "FLAGS");
        for (Product p : products) {
            System.out.printf("%-14s %-28s %-11s %9s %6d %s%n", p.sku(), p.name(), p.type(),
                    p.unitPrice(), p.quantity(), p.isLowStock() ? "LOW" : "");
        }
    }

    private static void history(List<StockMovement> list) {
        if (list.isEmpty()) { System.out.println("(no movements)"); return; }
        list.forEach(m -> System.out.printf("%s  %-10s %+6d  %s%n", m.timestamp(), m.type(), m.delta(), m.note()));
    }
}

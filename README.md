# Inventory Management (Java 17, Maven)

Stock tracking with an audit trail, low-stock alerts, expiry handling and file persistence. No runtime dependencies.

## Run
    mvn package
    java -jar target/inventory-1.0.0.jar [dataDir]     # default ./data

    > add W-1 2.50 10 3 HARDWARE Brass widget
    > add MILK 1.20 20 5 GROCERY 2026-12-31 Whole milk
    > receive W-1 5 PO-1001
    > issue W-1 12 Order 7        # triggers a LOW STOCK warning
    > low | list | value | history W-1 | adjust W-1 4 Cycle count

## Architecture
| Package | Role | OOP concept |
|---|---|---|
| `model` | `Product` (abstract) -> `StandardProduct`, `PerishableProduct`; `StockMovement` record | encapsulation, inheritance, polymorphism |
| `repository` | `ProductRepository`, `MovementLog` interfaces; in-memory and file implementations | abstraction, swappable storage |
| `service` | `InventoryService`: all business rules, locking, audit | single responsibility |
| `alert` | `StockAlertListener` observer | interfaces, observer pattern |
| `exception` | typed domain errors | custom exceptions |

## Production-minded details
- Validation in one place (`Validators`): SKU format, no control characters, non-negative money/quantities, overflow-safe stock.
- Copy-on-write operations: mutate a copy, persist, then audit; failures leave stored state unchanged.
- Atomic file writes (temp file + atomic move) with in-memory rollback if the write fails; corrupt data files fail loudly.
- Thread-safe service (lock) and repositories (synchronized); injectable `Clock` for deterministic tests.
- Append-only movement log for traceability; alerts fire only when stock crosses the threshold.

## Next steps for real deployments
Swap the file repositories for JDBC/JPA (implement `ProductRepository`), add a REST layer (Spring Boot/Javalin) over `InventoryService`, replace `java.util.logging` with SLF4J, and add suppliers/purchase orders.

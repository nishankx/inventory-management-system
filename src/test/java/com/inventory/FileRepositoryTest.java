package com.inventory;

import static org.junit.jupiter.api.Assertions.*;

import com.inventory.exception.RepositoryException;
import com.inventory.model.*;
import com.inventory.repository.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileRepositoryTest {
    @TempDir Path dir;

    @Test void dataSurvivesRestart() {
        Path f = dir.resolve("products.tsv");
        FileProductRepository repo = new FileProductRepository(f);
        repo.save(new StandardProduct("A1", "Cable", Category.ELECTRONICS, new BigDecimal("4.99"), 8, 2));
        repo.save(new PerishableProduct("M1", "Milk", Category.GROCERY, new BigDecimal("1.10"), 4, 1,
                LocalDate.of(2027, 1, 5)));

        FileProductRepository reloaded = new FileProductRepository(f);
        assertEquals(2, reloaded.findAll().size());
        assertEquals(LocalDate.of(2027, 1, 5),
                ((PerishableProduct) reloaded.findBySku("M1").orElseThrow()).expiryDate());
    }

    @Test void corruptFileFailsLoudly() throws Exception {
        Path f = dir.resolve("products.tsv");
        Files.writeString(f, "garbage line\n");
        assertThrows(RepositoryException.class, () -> new FileProductRepository(f));
    }

    @Test void movementLogRoundTrips() {
        FileMovementLog log = new FileMovementLog(dir.resolve("m.tsv"));
        log.append(new StockMovement(java.time.Instant.now(), "A1", MovementType.RECEIPT, 5, "note\twith tab"));
        assertEquals(1, log.history("A1").size());
        assertEquals("note with tab", log.history("A1").get(0).note());
    }
}

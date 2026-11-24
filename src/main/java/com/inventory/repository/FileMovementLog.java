package com.inventory.repository;

import com.inventory.exception.RepositoryException;
import com.inventory.model.MovementType;
import com.inventory.model.StockMovement;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Append-only audit trail; one tab-separated line per movement. */
public class FileMovementLog implements MovementLog {
    private final Path file;

    public FileMovementLog(Path file) { this.file = file; }

    @Override public synchronized void append(StockMovement m) {
        String note = m.note() == null ? "" : m.note().replaceAll("[\\t\\r\\n]", " ");
        String line = String.join("\t", m.timestamp().toString(), m.sku(), m.type().name(),
                String.valueOf(m.delta()), note) + System.lineSeparator();
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RepositoryException("Cannot append to " + file, e);
        }
    }

    @Override public synchronized List<StockMovement> history(String sku) {
        List<StockMovement> result = new ArrayList<>();
        if (!Files.exists(file)) return result;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] f = line.split("\t", -1);
                if (f.length == 5 && f[1].equals(sku)) {
                    result.add(new StockMovement(Instant.parse(f[0]), f[1],
                            MovementType.valueOf(f[2]), Integer.parseInt(f[3]), f[4]));
                }
            }
        } catch (IOException e) {
            throw new RepositoryException("Cannot read " + file, e);
        }
        return result;
    }
}

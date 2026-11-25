package com.tradex.utils;

import com.tradex.models.Transaction;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Writes transaction reports as CSV (file handling). */
public final class ReportExporter {
    private ReportExporter() {}

    public static void exportTransactions(List<Transaction> txs, Path file) throws IOException {
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write("Time,Symbol,Side,Quantity,Price,Total\n");
            for (Transaction t : txs) {
                w.write(String.join(",", csv(f.format(t.executedAt())), csv(t.symbol()), t.side().name(),
                        String.valueOf(t.quantity()), t.price().toPlainString(), t.total().toPlainString()));
                w.write("\n");
            }
        }
    }

    /** Quotes a field if needed and neutralises spreadsheet formula injection. */
    static String csv(String s) {
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        return s.contains(",") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }
}

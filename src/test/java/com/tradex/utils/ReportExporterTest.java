package com.tradex.utils;

import com.tradex.models.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportExporterTest {
    @Test void writesHeaderAndRows() throws Exception {
        Path f = Files.createTempFile("tradex", ".csv");
        try {
            var t = new Transaction(1, 1, 1, "TCS", Side.BUY, 3, new BigDecimal("2515.40"), LocalDateTime.of(2026, 1, 2, 3, 4, 5));
            ReportExporter.exportTransactions(List.of(t), f);
            List<String> lines = Files.readAllLines(f);
            assertEquals("Time,Symbol,Side,Quantity,Price,Total", lines.get(0));
            assertEquals("2026-01-02 03:04:05,TCS,BUY,3,2515.40,7546.20", lines.get(1));
        } finally { Files.deleteIfExists(f); }
    }

    @Test void escapesSpecialCharacters() {
        assertEquals("\"a,b\"", ReportExporter.csv("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", ReportExporter.csv("say \"hi\""));
        assertEquals("'=SUM(A1)", ReportExporter.csv("=SUM(A1)"));
        assertEquals("plain", ReportExporter.csv("plain"));
    }
}

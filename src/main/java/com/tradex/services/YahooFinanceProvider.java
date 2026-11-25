package com.tradex.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradex.exceptions.MarketDataException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Fetches NSE prices from Yahoo Finance's unofficial chart endpoint (no API key). It can change without notice. */
public class YahooFinanceProvider implements MarketDataProvider {
    private static final String URL = "https://query1.finance.yahoo.com/v8/finance/chart/%s.NS?interval=1d&range=1d";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @Override
    public Map<String, BigDecimal> fetchPrices(Collection<String> symbols) throws MarketDataException {
        Map<String, CompletableFuture<BigDecimal>> pending = new LinkedHashMap<>();
        for (String sym : symbols) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(URL.formatted(URLEncoder.encode(sym, StandardCharsets.UTF_8))))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "Mozilla/5.0 (TradeX student project)")
                    .build();
            pending.put(sym, http.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenApply(r -> {
                if (r.statusCode() != 200) return null; // e.g. unknown/renamed ticker
                try { return parsePrice(r.body()); } catch (MarketDataException e) { return null; }
            }).exceptionally(t -> null));
        }
        Map<String, BigDecimal> out = new HashMap<>();
        pending.forEach((sym, f) -> { BigDecimal p = f.join(); if (p != null) out.put(sym, p); });
        if (out.isEmpty() && !symbols.isEmpty()) throw new MarketDataException("No prices received from Yahoo Finance");
        return out;
    }

    static BigDecimal parsePrice(String json) throws MarketDataException {
        try {
            JsonNode n = JSON.readTree(json).path("chart").path("result").path(0).path("meta").path("regularMarketPrice");
            if (!n.isNumber() || n.decimalValue().signum() <= 0) throw new MarketDataException("Price missing in response");
            return n.decimalValue().setScale(2, RoundingMode.HALF_UP);
        } catch (IOException e) {
            throw new MarketDataException("Malformed response", e);
        }
    }
}

package org.sokei.apps.orderbook;

import java.util.*;

public class OrderBook {
    private long lastUpdateId;

    private final TreeMap<Double, Double> asks;  // Sorted ascending
    private final TreeMap<Double, Double> bids; // Sorted descending

    private final List<PriceLevel> mutableAsks;
    private final List<PriceLevel> mutableBids;

    public OrderBook() {
        lastUpdateId = 0;
        asks = new TreeMap<>();
        bids = new TreeMap<>(Collections.reverseOrder());
        this.mutableAsks = new ArrayList<>();
        this.mutableBids = new ArrayList<>();
    }

    public void setBids(final List<PriceLevel> bids) {
        for (PriceLevel bid : bids) {
            this.bids.put(bid.price, bid.quantity);
        }
    }

    public void setAsks(final List<PriceLevel> asks) {
        for (PriceLevel ask : asks) {
            this.asks.put(ask.price, ask.quantity);
        }
    }

    public void updateBid(Double price, Double quantity) {
        this.bids.put(price, quantity);
    }

    public void updateAsk(Double price, Double quantity) {
        this.asks.put(price, quantity);
    }

    public void update(OrderBook newBook) {

        // Update asks
        for (PriceLevel ask : newBook.getAsks()) {
            if (ask.quantity == 0) {
                asks.remove(ask.price); // Remove zero quantity
            } else {
                asks.put(ask.price, ask.quantity);
            }
        }

        // Update bids
        for (PriceLevel bid : newBook.getBids()) {
            if (bid.quantity == 0) {
                bids.remove(bid.price);
            } else {
                bids.put(bid.price, bid.quantity);
            }
        }
    }

    public List<PriceLevel> getAsks() {
        mutableAsks.clear();
        for (Map.Entry<Double, Double> entry : asks.entrySet()) {
            mutableAsks.add(new PriceLevel(entry.getKey(), entry.getValue()));
        }
        return mutableAsks;
    }

    public List<PriceLevel> getBids() {
        mutableBids.clear();
        for (Map.Entry<Double, Double> entry : bids.entrySet()) {
            mutableBids.add(new PriceLevel(entry.getKey(), entry.getValue()));
        }
        return mutableBids;
    }

    public void print(int depth) {
        System.out.println("📊 ORDER BOOK (Top " + depth + " levels)");

        System.out.println("🔴 ASKS (SELL)");
        asks.entrySet().stream().limit(depth).forEach(e -> System.out.printf("%.2f -> %.6f%n", e.getKey(), e.getValue()));

        System.out.println("🟢 BIDS (BUY)");
        bids.entrySet().stream().limit(depth).forEach(e -> System.out.printf("%.2f -> %.6f%n", e.getKey(), e.getValue()));
    }

    public void clear() {
        asks.clear();
        bids.clear();
        lastUpdateId = 0;
    }
}
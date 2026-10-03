package com.modernlife.service;

import java.util.HashMap;
import java.util.Map;

public class BillingService {
    private final Map<String, Double> taxRates = new HashMap<>();
    private final Map<String, Double> balances = new HashMap<>();

    public void setTaxRate(String itemId, double rate) {
        taxRates.put(itemId, rate);
    }

    public void processDailyTaxes() {
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            String itemId = entry.getKey();
            double balance = entry.getValue();
            if (balance > 0) {
                double taxAmount = balance * taxRates.getOrDefault(itemId, 0.0);
                // Deduct tax from balance
                balances.put(itemId, balance - taxAmount);
            }
        }
    }

    public void addBalance(String itemId, double amount) {
        balances.put(itemId, balances.getOrDefault(itemId, 0.0) + amount);
    }

    public double getBalance(String itemId) {
        return balances.getOrDefault(itemId, 0.0);
    }
}
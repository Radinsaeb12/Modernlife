package com.modernlife.service;

import java.util.HashMap;
import java.util.Map;

public class PenaltyService {
    private final Map<String, Integer> penalties = new HashMap<>();

    public void setPenalty(String userId, int penalty) {
        penalties.put(userId, penalty);
    }

    public void applyPenalties() {
        for (Map.Entry<String, Integer> entry : penalties.entrySet()) {
            String userId = entry.getKey();
            int penalty = entry.getValue();
            if (penalty > 0) {
                // Apply penalty to user
                System.out.println("Applying penalty of " + penalty + " to user " + userId);
            }
        }
    }

    public void resetPenalties() {
        penalties.clear();
    }
}// No changes needed

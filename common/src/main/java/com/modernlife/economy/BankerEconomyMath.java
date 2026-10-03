package com.modernlife.economy;

import com.modernlife.data.world.ModernLifeWorldData;

public class BankerEconomyMath {
    public record TradeResult(boolean isValid, int safeQuantity, double totalPayment) {}

    public static TradeResult calculateSafeTransaction(double unitPrice, int count, double budgetCap) {
        if (unitPrice <= 0 || count <= 0) {
            return new TradeResult(false, 0, 0);
        }

        int maxPossible = (int) Math.min(count, Math.floor(budgetCap / unitPrice));
        
        for (int safeCount = maxPossible; safeCount > 0; safeCount--) {
            double total = safeCount * unitPrice;
            
            if (total >= 5.0 && total % 5.0 == 0) {
                return new TradeResult(true, safeCount, total);
            }
        }

        return new TradeResult(false, 0, 0);
    }

    /**
     * YENİ: Oyuncunun Bankacıya sattığı eşyanın zorluğa göre hesaplanıp 5'e yuvarlanmış fiyatı.
     */
    public static long calculateBankerPrice(long basePrice, int itemAmount, ModernLifeWorldData.DifficultyLevel difficulty) {
        if (basePrice <= 0 || itemAmount <= 0) return 0;
        
        double multiplier = difficulty.getMultiplier();
        double rawTotalPrice = (basePrice * itemAmount) * multiplier;
        
        // En yakın 5'in katına yuvarlama (Örn: 12.5 -> 15 | 8.2 -> 10)
        long roundedPrice = Math.round(rawTotalPrice / 5.0) * 5;
        
        // Fiyat 5 TL'nin altına düşemez
        return Math.max(5L, roundedPrice);
    }
}
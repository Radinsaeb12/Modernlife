package com.modernlife.economy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class BankerInventoryScanner {
    public record SellableItemData(Item item, int count, double unitPrice, double maxBudgetCap) {}

    public static Map<Item, SellableItemData> getPlayerSellableItems(ServerPlayer player) {
        Map<Item, SellableItemData> sellables = new HashMap<>();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                double price = EconomyRegistry.getItemPrice(item);
                if (price > 0) {
                    SellableItemData existing = sellables.get(item);
                    int currentCount = (existing != null) ? existing.count() : 0;
                    sellables.put(item, new SellableItemData(item, currentCount + stack.getCount(), price, 100000.0));
                }
            }
        }
        return sellables;
    }
}
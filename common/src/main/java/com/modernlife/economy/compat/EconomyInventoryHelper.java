package com.modernlife.economy.compat;

import com.modernlife.registry.ModItems;
import com.modernlife.service.EconomyService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EconomyInventoryHelper {

    private EconomyInventoryHelper() {}

    private static final String[] COIN_IDS = {
        "lightmanscurrency:coin_netherite",
        "lightmanscurrency:coin_diamond",
        "lightmanscurrency:coin_emerald",
        "lightmanscurrency:coin_gold",
        "lightmanscurrency:coin_iron",
        "lightmanscurrency:coin_copper"
    };

    // ModernLife özel dengeleme oranları: 1 Netherite = 3.600
    private static final long[] COIN_VALUES = {
        3600L, 1000L, 250L, 50L, 5L, 1L
    };

    public static long getInventoryTotalCash(ServerPlayer player) {
        if (EconomyService.isLightmansActive(player)) {
            long total = 0L;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty()) continue;
                ResourceLocation regName = BuiltInRegistries.ITEM.getKey(stack.getItem());
                String id = regName.toString();
                for (int c = 0; c < COIN_IDS.length; c++) {
                    if (COIN_IDS[c].equals(id)) {
                        total += COIN_VALUES[c] * stack.getCount();
                        break;
                    }
                }
            }
            return total;
        } else {
            long total = 0L;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                Item item = stack.getItem();
                int count = stack.getCount();
                if (item == ModItems.TL_5.get()) total += (5L * count);
                else if (item == ModItems.TL_10.get()) total += (10L * count);
                else if (item == ModItems.TL_20.get()) total += (20L * count);
                else if (item == ModItems.TL_50.get()) total += (50L * count);
                else if (item == ModItems.TL_100.get()) total += (100L * count);
                else if (item == ModItems.TL_200.get()) total += (200L * count);
            }
            return total;
        }
    }

    public static boolean deductCash(ServerPlayer player, long amount) {
        if (getInventoryTotalCash(player) < amount) return false;

        if (EconomyService.isLightmansActive(player)) {
            long remaining = amount;
            for (int c = 0; c < COIN_IDS.length; c++) {
                ResourceLocation loc = ResourceLocation.tryParse(COIN_IDS[c]);
                if (loc == null) continue;
                Item coinItem = BuiltInRegistries.ITEM.get(loc);
                if (coinItem == Items.AIR) continue;
                long val = COIN_VALUES[c];

                for (int s = 0; s < player.getInventory().getContainerSize(); s++) {
                    ItemStack stack = player.getInventory().getItem(s);
                    if (stack.getItem() == coinItem) {
                        while (!stack.isEmpty() && remaining >= val) {
                            stack.shrink(1);
                            remaining -= val;
                        }
                    }
                }
            }

            if (remaining > 0) {
                for (int c = COIN_IDS.length - 1; c >= 0; c--) {
                    long val = COIN_VALUES[c];
                    if (val > remaining) {
                        ResourceLocation loc = ResourceLocation.tryParse(COIN_IDS[c]);
                        if (loc == null) continue;
                        Item coinItem = BuiltInRegistries.ITEM.get(loc);
                        for (int s = 0; s < player.getInventory().getContainerSize(); s++) {
                            ItemStack stack = player.getInventory().getItem(s);
                            if (stack.getItem() == coinItem && !stack.isEmpty()) {
                                stack.shrink(1);
                                long change = val - remaining;
                                giveCash(player, change);
                                remaining = 0;
                                break;
                            }
                        }
                    }
                    if (remaining == 0) break;
                }
            }
            return true;
        } else {
            long remaining = amount;
            Item[] cashItems = { ModItems.TL_200.get(), ModItems.TL_100.get(), ModItems.TL_50.get(), ModItems.TL_20.get(), ModItems.TL_10.get(), ModItems.TL_5.get() };
            long[] cashValues = { 200L, 100L, 50L, 20L, 10L, 5L };

            for (int i = 0; i < cashItems.length; i++) {
                long value = cashValues[i];
                Item cashItem = cashItems[i];
                for (int s = 0; s < player.getInventory().getContainerSize(); s++) {
                    ItemStack stack = player.getInventory().getItem(s);
                    if (stack.getItem() == cashItem) {
                        while (!stack.isEmpty() && remaining >= value) {
                            stack.shrink(1);
                            remaining -= value;
                        }
                    }
                }
            }

            if (remaining > 0) {
                for (int i = 0; i < cashItems.length; i++) {
                    long value = cashValues[i];
                    Item cashItem = cashItems[i];
                    if (value > remaining) {
                        for (int s = 0; s < player.getInventory().getContainerSize(); s++) {
                            ItemStack stack = player.getInventory().getItem(s);
                            if (stack.getItem() == cashItem && !stack.isEmpty()) {
                                stack.shrink(1);
                                long change = value - remaining;
                                giveCash(player, change);
                                remaining = 0;
                                break;
                            }
                        }
                    }
                    if (remaining == 0) break;
                }
            }
            return true;
        }
    }

    public static void giveCash(ServerPlayer player, long amount) {
        if (amount <= 0) return;

        if (EconomyService.isLightmansActive(player)) {
            long remaining = amount;
            for (int c = 0; c < COIN_IDS.length; c++) {
                long val = COIN_VALUES[c];
                long count = remaining / val;
                if (count > 0) {
                    ResourceLocation loc = ResourceLocation.tryParse(COIN_IDS[c]);
                    if (loc != null) {
                        Item coinItem = BuiltInRegistries.ITEM.get(loc);
                        if (coinItem != Items.AIR) {
                            while (count > 0) {
                                int toGive = (int) Math.min(count, 64);
                                ItemStack coinStack = new ItemStack(coinItem, toGive);
                                if (!player.getInventory().add(coinStack)) {
                                    player.drop(coinStack, false);
                                }
                                count -= toGive;
                            }
                        }
                    }
                    remaining %= val;
                }
            }
        } else {
            long remaining = amount;
            Item[] cashItems = { ModItems.TL_200.get(), ModItems.TL_100.get(), ModItems.TL_50.get(), ModItems.TL_20.get(), ModItems.TL_10.get(), ModItems.TL_5.get() };
            long[] cashValues = { 200L, 100L, 50L, 20L, 10L, 5L };

            for (int i = 0; i < cashValues.length; i++) {
                long billCount = remaining / cashValues[i];
                if (billCount > 0) {
                    while (billCount > 0) {
                        int toGive = (int) Math.min(billCount, 64);
                        ItemStack stack = new ItemStack(cashItems[i], toGive);
                        if (!player.getInventory().add(stack)) {
                            player.drop(stack, false);
                        }
                        billCount -= toGive;
                    }
                    remaining %= cashValues[i];
                }
            }
        }
    }
}
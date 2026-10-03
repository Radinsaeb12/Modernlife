package com.modernlife.economy.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

public class LightmansCurrencyBridge {

    public static boolean deposit(Player player, long amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        try {
            Class<?> moneyValueClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue");
            Method fromNumber = moneyValueClass.getMethod("fromNumber", String.class, double.class);
            Object moneyValue = fromNumber.invoke(null, "main", (double) amount);

            Class<?> moneyApiClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.MoneyAPI");
            Object apiInstance = moneyApiClass.getMethod("getAPI").invoke(null);

            Method addMoneyMethod = apiInstance.getClass().getMethod("addPlayerMoney", ServerPlayer.class, moneyValueClass);
            Object result = addMoneyMethod.invoke(apiInstance, serverPlayer, moneyValue);
            return result instanceof Boolean ? (Boolean) result : true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean withdraw(Player player, long amount) {
        if (!(player instanceof ServerPlayer serverPlayer)) return false;
        try {
            Class<?> moneyValueClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue");
            Method fromNumber = moneyValueClass.getMethod("fromNumber", String.class, double.class);
            Object moneyValue = fromNumber.invoke(null, "main", (double) amount);

            Class<?> moneyApiClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.MoneyAPI");
            Object apiInstance = moneyApiClass.getMethod("getAPI").invoke(null);

            Method removeMoneyMethod = apiInstance.getClass().getMethod("removePlayerMoney", ServerPlayer.class, moneyValueClass);
            Object result = removeMoneyMethod.invoke(apiInstance, serverPlayer, moneyValue);
            return result instanceof Boolean ? (Boolean) result : false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static long getBalance(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return 0L;
        try {
            Class<?> moneyApiClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.MoneyAPI");
            Object apiInstance = moneyApiClass.getMethod("getAPI").invoke(null);

            Method getPlayerMoney = apiInstance.getClass().getMethod("getPlayerMoney", ServerPlayer.class);
            Object moneyHolder = getPlayerMoney.invoke(apiInstance, serverPlayer);

            Method getAmount = moneyHolder.getClass().getMethod("getAmount", String.class);
            Object result = getAmount.invoke(moneyHolder, "main");
            return result instanceof Number ? ((Number) result).longValue() : 0L;
        } catch (Throwable t) {
            return 0L;
        }
    }

    public static Component formatValue(long amount) {
        try {
            Class<?> moneyValueClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue");
            Method fromNumber = moneyValueClass.getMethod("fromNumber", String.class, double.class);
            Object moneyValue = fromNumber.invoke(null, "main", (double) amount);

            Method getText = moneyValueClass.getMethod("getText");
            Object result = getText.invoke(moneyValue);
            if (result instanceof Component comp) {
                return comp;
            }
        } catch (Throwable ignored) {}
        return Component.translatable("gui.modernlife.currency_format", String.valueOf(amount));
    }
}
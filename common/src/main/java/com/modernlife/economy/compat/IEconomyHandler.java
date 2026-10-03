package com.modernlife.economy.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public interface IEconomyHandler {
    boolean deposit(Player player, String identity, long amount);
    boolean withdraw(Player player, String identity, long amount);
    long getBalance(Player player, String identity);
    Component formatValue(long amount);
}
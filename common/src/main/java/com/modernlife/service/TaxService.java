package com.modernlife.service;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.data.TaxRates;
import com.modernlife.data.world.ModernLifeWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class TaxService {

    private TaxService() {}

    public static void setTaxRates(MinecraftServer server, TaxRates taxRates) {
        if (server == null) return;
        ServerLevel overworld = server.overworld();
        if (overworld != null) {
            ModernLifeWorldData worldData = ModernLifeWorldData.get(overworld);
            worldData.setTaxRates(taxRates);
            WorldStateService.syncToAll(server);
        }
    }

    public static void collectTaxesForPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ModernLifeWorldData worldData = ModernLifeWorldData.get(level);

        EconomyService.TaxDueSummary summary = EconomyService.calculateTaxDueSummary(player);
        long totalTax = summary.totalToPay();

        if (totalTax <= 0L) {
            player.sendSystemMessage(Component.translatable("modernlife.tax.no_tax_due").withStyle(ChatFormatting.GREEN));
            return;
        }

        boolean withdrawn = EconomyService.withdraw(player, totalTax);

        if (withdrawn) {
            worldData.addStateTreasuryBalance(totalTax);

            // Yeni ortak capability üzerinden borç sıfırlama
            ModCapabilities.get(player).setTaxDue(0L);

            EconomyService.syncToClient(player);
            WorldStateService.syncToAll(level.getServer());

            Component formattedTotal = EconomyService.formatMoney(player, totalTax);
            player.sendSystemMessage(
                Component.translatable("modernlife.tax.collected_success", formattedTotal).withStyle(ChatFormatting.GREEN)
            );
        } else {
            Component formattedTotal = EconomyService.formatMoney(player, totalTax);
            player.sendSystemMessage(
                Component.translatable("modernlife.tax.collected_failed", formattedTotal).withStyle(ChatFormatting.RED)
            );
        }
    }
}
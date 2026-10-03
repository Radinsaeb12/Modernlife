package com.modernlife.network.packet;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import com.modernlife.data.world.ModernLifeWorldData;
import com.modernlife.network.ModNetwork;
import com.modernlife.service.EconomyService;
import com.modernlife.service.WorldStateService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public record PayTaxesPacket(String identity) {
    public static void encode(PayTaxesPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.identity, 256);
    }

    public static PayTaxesPacket decode(FriendlyByteBuf buffer) {
        return new PayTaxesPacket(buffer.readUtf(256));
    }

    public static void handle(PayTaxesPacket packet, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext context = ctxSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            if (EconomyService.isTreasury(packet.identity())) {
                player.displayClientMessage(Component.translatable("modernlife.bank.treasury_no_tax"), false);
                return;
            }

            PlayerEconomy economy = ModCapabilities.get(player);
            if (economy == null) return;

            if (economy.getTaxDue() <= 0) {
                player.displayClientMessage(Component.translatable("modernlife.bank.no_tax_due"), false);
                return;
            }

            final EconomyService.TaxDueSummary summary = EconomyService.calculateTaxDueSummary(player);
            final long finalTotalToPay = summary.totalToPay();

            final StringBuilder taxBreakdown = new StringBuilder();
            taxBreakdown.append(Component.translatable("modernlife.tax.living_tax_breakdown", summary.livingTax()).getString());
            if (summary.vehicleTax() > 0) {
                taxBreakdown.append("\n").append(Component.translatable("modernlife.tax.vehicle_tax_breakdown", summary.vehicleTax()).getString());
            }
            if (summary.mountTax() > 0) {
                taxBreakdown.append("\n").append(Component.translatable("modernlife.tax.mount_tax_breakdown", summary.mountTax()).getString());
            }
            if (summary.penaltyAmount() > 0) {
                taxBreakdown.append("\n").append(Component.translatable("modernlife.tax.penalty_breakdown", summary.penaltyAmount()).getString());
            }

            if (finalTotalToPay <= 0) {
                economy.setTaxDue(0);
                economy.setTaxDueAmount(0);
                ModCapabilities.save(player);
                EconomyService.syncToClient(player);
                player.displayClientMessage(Component.translatable("modernlife.gui.bank.no_taxes"), false);
                return;
            }

            if (EconomyService.hasValidBankCard(player, packet.identity)
                    && EconomyService.withdraw(player, packet.identity, finalTotalToPay)) {

                economy.setTaxDue(0);
                economy.setTaxDueAmount(0);
                ModCapabilities.save(player);

                ModernLifeWorldData worldData = WorldStateService.getWorldData(player.serverLevel());
                worldData.addStateTreasuryBalance(finalTotalToPay);
                worldData.setDirty();

                WorldStateService.syncToAll(player.server);
                for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                    EconomyService.syncToClient(p);
                }

                Component playerReceipt = Component.translatable("modernlife.transaction.tax_payment", finalTotalToPay);
                ModNetwork.CHANNEL.sendToPlayer(player, new AddTransactionPacket(packet.identity(), playerReceipt));

                Component treasuryReceipt = Component.translatable("modernlife.transaction.tax_revenue", packet.identity(), finalTotalToPay);
                ModNetwork.CHANNEL.sendToPlayers(player.server.getPlayerList().getPlayers(), 
                        new AddTransactionPacket(EconomyService.TREASURY_ID, treasuryReceipt));

                player.displayClientMessage(
                    Component.translatable("modernlife.tax.paid_summary", finalTotalToPay, taxBreakdown.toString()),
                    false);

            } else {
                player.displayClientMessage(Component.translatable("modernlife.bank.not_enough_money_tax"), false);
            }
        });
    }
}
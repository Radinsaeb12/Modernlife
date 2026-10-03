package com.modernlife.network.packet;

import com.modernlife.capability.ModCapabilities;
import com.modernlife.capability.PlayerEconomy;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public record SyncPlayerEconomyPacket(Map<String, Long> identityBalances, long taxDue, long taxDueAmount) {
    public static void encode(final SyncPlayerEconomyPacket packet, final FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.identityBalances.size());
        packet.identityBalances.forEach((identity, balance) -> {
            buffer.writeUtf(identity, 256);
            buffer.writeLong(balance);
        });
        buffer.writeLong(packet.taxDue());
        buffer.writeLong(packet.taxDueAmount());
    }

    public static SyncPlayerEconomyPacket decode(final FriendlyByteBuf buffer) {
        final int count = buffer.readVarInt();
        final Map<String, Long> balances = new HashMap<>();
        for (int i = 0; i < count; i++) balances.put(buffer.readUtf(256), buffer.readLong());
        final long taxDue = buffer.readLong();
        final long taxDueAmount = buffer.readLong();
        return new SyncPlayerEconomyPacket(balances, taxDue, taxDueAmount);
    }

    public static void handle(final SyncPlayerEconomyPacket packet, final Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext context = ctxSupplier.get();
        context.queue(() -> {
            final var player = Minecraft.getInstance().player;
            if (player != null) {
                PlayerEconomy economy = ModCapabilities.get(player);
                economy.replaceIdentityBalances(packet.identityBalances());
                economy.setTaxDue(packet.taxDue());
                economy.setTaxDueAmount(packet.taxDueAmount());
            }
        });
    }
}
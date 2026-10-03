package com.modernlife.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public record SyncBalancePacket(long balance) {
    public static void encode(SyncBalancePacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.balance());
    }

    public static SyncBalancePacket decode(FriendlyByteBuf buffer) {
        return new SyncBalancePacket(buffer.readLong());
    }

    public static void handle(SyncBalancePacket packet, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext context = ctxSupplier.get();
        context.queue(() -> {
            // İstemci tarafında bakiye güncellemesi yapılacaksa buraya bağlanır
        });
    }
}
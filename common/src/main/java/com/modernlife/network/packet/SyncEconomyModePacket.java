package com.modernlife.network.packet;

import com.modernlife.economy.compat.ClientEconomyState;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class SyncEconomyModePacket {
    private final boolean isLightmans;

    public SyncEconomyModePacket(boolean isLightmans) {
        this.isLightmans = isLightmans;
    }

    public static void encode(SyncEconomyModePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isLightmans);
    }

    public static SyncEconomyModePacket decode(FriendlyByteBuf buf) {
        return new SyncEconomyModePacket(buf.readBoolean());
    }

    public static void handle(SyncEconomyModePacket msg, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ClientEconomyState.isLightmansMode = msg.isLightmans;
        });
    }
}
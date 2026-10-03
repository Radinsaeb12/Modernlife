package com.modernlife.network.packet;

import com.modernlife.service.EconomyService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class EnsureIdentityPacket {

    public EnsureIdentityPacket() {}

    public EnsureIdentityPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public static void handle(EnsureIdentityPacket packet, Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer sender = (ServerPlayer) ctx.getPlayer();
            if (sender == null) return;

            EconomyService.getOrCreateIdentity(sender);
            EconomyService.syncToClient(sender);
        });
    }
}
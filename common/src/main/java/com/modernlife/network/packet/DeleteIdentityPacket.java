package com.modernlife.network.packet;

import com.modernlife.service.EconomyService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class DeleteIdentityPacket {
    private final String identityName;

    public DeleteIdentityPacket(String identityName) {
        this.identityName = identityName;
    }

    public DeleteIdentityPacket(FriendlyByteBuf buf) {
        this.identityName = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.identityName);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer player = (ServerPlayer) ctx.getPlayer();
            if (player != null) {
                boolean success = EconomyService.cancelIdentity(player, this.identityName);
                if (success) {
                    player.sendSystemMessage(Component.translatable("modernlife.edevlet.cancel_success", this.identityName));
                } else {
                    player.sendSystemMessage(Component.translatable("modernlife.edevlet.cancel_failure"));
                }
            }
        });
    }
}
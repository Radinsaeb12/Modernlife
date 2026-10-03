package com.modernlife.network.packet;

import com.modernlife.service.PhoneCallService;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class C2SPhoneCallPacket {
    public enum Action { CALL, ACCEPT, DECLINE }

    private final Action action;
    private final String targetPlayerName;

    public C2SPhoneCallPacket(Action action, String targetPlayerName) {
        this.action = action;
        this.targetPlayerName = targetPlayerName == null ? "" : targetPlayerName;
    }

    public C2SPhoneCallPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.targetPlayerName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(targetPlayerName);
    }

    public void handle(Supplier<NetworkManager.PacketContext> ctxSupplier) {
        NetworkManager.PacketContext ctx = ctxSupplier.get();
        ctx.queue(() -> {
            ServerPlayer sender = (ServerPlayer) ctx.getPlayer();
            if (sender == null) return;

            switch (action) {
                case CALL -> {
                    if (!targetPlayerName.isEmpty()) {
                        ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(targetPlayerName);
                        if (target != null && target != sender) {
                            PhoneCallService.initiateCallRequest(sender, target);
                        }
                    }
                }
                case ACCEPT -> {
                    PhoneCallService.acceptCall(sender);
                }
                case DECLINE -> {
                    PhoneCallService.declineOrEndCall(sender);
                }
            }
        });
    }
}